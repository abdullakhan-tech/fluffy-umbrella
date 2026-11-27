import { createRequire } from "node:module";
import assert from "node:assert/strict";
import { test, before, after, beforeEach } from "node:test";
import fs from "node:fs";

// The repository root is an ES module package for the Zaythar Office server.
// Firebase Rules Unit Testing is installed globally in CI, so use createRequire
// to retain NODE_PATH-based CommonJS resolution without changing package scope.
const require = createRequire(import.meta.url);
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

test("Firebase Rules Unit Testing helpers load under the repository ESM package mode", () => {
  assert.equal(typeof initializeTestEnvironment, "function");
  assert.equal(typeof assertFails, "function");
  assert.equal(typeof assertSucceeds, "function");
});

function pastTimestamp(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. UNAUTHENTICATED REJECTIONS ---

test("Unauthenticated user: cannot read or write profiles, chat_sessions, session messages, plugins, brain_runs, memories, or knowledge facts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      operatorName: "Hacker",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chat_sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("s1").collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("plugins").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("brain_runs").get());
});

// --- 2. CROSS-USER ISOLATION ---

test("Authenticated user: cannot list /users root collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated user: cannot read, create, update, or delete another user's profile or subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = pastTimestamp();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      operatorName: "Bob",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("chat_sessions").doc("s1").set({
      userId: BOB_UID,
      title: "Bob Session",
      agent: "general",
      lastMessagePreview: "Secret",
      messageCount: 1,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("chat_sessions").doc("s1").collection("messages").doc("sm1").set({
      userId: BOB_UID,
      sessionId: "s1",
      role: "user",
      text: "Bob session secret",
      agent: "general",
      modelLabel: "",
      hasImage: false,
      webSources: [],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("plugins").doc("plug1").set({
      userId: BOB_UID,
      pluginKey: "web_search",
      name: "Web Search",
      description: "Internet search",
      category: "Internet",
      enabled: true,
      systemInstruction: "",
      isBuiltIn: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("brain_runs").doc("br1").set({
      userId: BOB_UID,
      mode: "reasoner",
      prompt: "Analyze Bob plan",
      summary: "Private result",
      confidence: 88,
      insights: ["Step 1"],
      suggestedTriplesCount: 1,
      modelUsed: "gemini-3.8-flash",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("chat_sessions").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("chat_sessions").doc("s1").collection("messages").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("plugins").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("brain_runs").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("plugins").doc("plug2").set({
      userId: ALICE_UID,
      pluginKey: "hijack",
      name: "Hijack",
      description: "Desc",
      category: "Custom",
      enabled: true,
      systemInstruction: "",
      isBuiltIn: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user cannot read or mutate another user's memory notes or knowledge facts", async () => {
  const ts = pastTimestamp();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("memories").doc("mem_private").set({
      userId: BOB_UID,
      content: "Synthetic private memory",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("knowledge_facts").doc("fact_private").set({
      userId: BOB_UID,
      subject: "Synthetic subject",
      predicate: "has",
      objectValue: "private value",
      source: "test",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobMemories = aliceDb.collection("users").doc(BOB_UID).collection("memories");
  const bobMemory = bobMemories.doc("mem_private");
  await assertFails(bobMemories.get());
  await assertFails(bobMemory.get());
  await assertFails(bobMemory.update({
    content: "Attempted overwrite",
    updatedAt: pastTimestamp(1000),
  }));
  await assertFails(bobMemory.delete());
  await assertFails(bobMemories.doc("mem_injected").set({
    userId: ALICE_UID,
    content: "Attempted cross-account create",
    createdAt: ts,
    updatedAt: ts,
  }));

  const bobFacts = aliceDb.collection("users").doc(BOB_UID).collection("knowledge_facts");
  const bobFact = bobFacts.doc("fact_private");
  await assertFails(bobFacts.get());
  await assertFails(bobFact.get());
  await assertFails(bobFact.update({
    predicate: "attempted overwrite",
    updatedAt: pastTimestamp(1000),
  }));
  await assertFails(bobFact.delete());
  await assertFails(bobFacts.doc("fact_injected").set({
    userId: ALICE_UID,
    subject: "Attempted cross-account create",
    predicate: "has",
    objectValue: "no access",
    source: "test",
    createdAt: ts,
    updatedAt: ts,
  }));
});

// --- 3. SCHEMA, GHOST FIELDS, SPOOFING & RESOURCE POISONING ---

test("Schema guard: rejects ghost fields, spoofed userId, mismatched sessionId, oversized arrays/strings, and invalid pluginKey", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  // Ghost field in chat_session
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("s1").set({
      userId: ALICE_UID,
      title: "Valid Title",
      agent: "general",
      lastMessagePreview: "Hi",
      messageCount: 0,
      adminOverride: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Mismatched sessionId in session message
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("s1").collection("messages").doc("m1").set({
      userId: ALICE_UID,
      sessionId: "other_session",
      role: "user",
      text: "Hello",
      agent: "general",
      modelLabel: "",
      hasImage: false,
      webSources: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Oversized webSources array (> 10)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("s1").collection("messages").doc("m2").set({
      userId: ALICE_UID,
      sessionId: "s1",
      role: "assistant",
      text: "Grounded answer",
      agent: "research",
      modelLabel: "gemini-3.8-flash",
      hasImage: false,
      webSources: Array.from({ length: 11 }, (_, i) => `https://example.com/${i}`),
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid pluginKey characters
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("plugins").doc("p_bad").set({
      userId: ALICE_UID,
      pluginKey: "bad key!",
      name: "Bad Plugin",
      description: "Desc",
      category: "Custom",
      enabled: true,
      systemInstruction: "",
      isBuiltIn: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Out-of-range brain_runs confidence (> 100)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("brain_runs").doc("b_bad").set({
      userId: ALICE_UID,
      mode: "reasoner",
      prompt: "Analyze",
      summary: "Summary",
      confidence: 150,
      insights: ["Insight"],
      suggestedTriplesCount: 0,
      modelUsed: "gemini-3.8-flash",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Immutability guard: rejects modifying immutable fields on sessions, plugins, and brain_runs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp(8000);

  const plugRef = aliceDb.collection("users").doc(ALICE_UID).collection("plugins").doc("p1");
  await assertSucceeds(
    plugRef.set({
      userId: ALICE_UID,
      pluginKey: "calculator",
      name: "Calculator",
      description: "Math evaluator",
      category: "Compute",
      enabled: true,
      systemInstruction: "Evaluate math accurately.",
      isBuiltIn: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot mutate pluginKey or isBuiltIn
  await assertFails(plugRef.update({ pluginKey: "other", updatedAt: pastTimestamp(1000) }));
  await assertFails(plugRef.update({ isBuiltIn: false, updatedAt: pastTimestamp(1000) }));

  // brain_runs is append-only (no updates)
  const brainRef = aliceDb.collection("users").doc(ALICE_UID).collection("brain_runs").doc("br1");
  await assertSucceeds(
    brainRef.set({
      userId: ALICE_UID,
      mode: "reasoner",
      prompt: "Optimize latency",
      summary: "Use coroutine concurrency",
      confidence: 91,
      insights: ["Profile IO", "Batch Firestore reads"],
      suggestedTriplesCount: 2,
      modelUsed: "gemini-3.8-flash",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(brainRef.update({ confidence: 99, updatedAt: pastTimestamp(500) }));
});

// --- 4. OWNER CRUD SUCCESS PATHS ---

test("Authenticated owner: full CRUD lifecycle for chat_sessions, session messages, plugins, brain_runs, profile, memories, and knowledge_facts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp(5000);

  // Profile
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set({ userId: ALICE_UID, operatorName: "OPERATOR 01", createdAt: ts, updatedAt: ts }));
  await assertSucceeds(profileRef.update({ operatorName: "COMMANDER", updatedAt: pastTimestamp(1000) }));

  // Chat Session + Session Messages
  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("session_1");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "Mission Brief",
      agent: "general",
      lastMessagePreview: "",
      messageCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    sessionRef.update({
      title: "Mission Brief · Updated",
      lastMessagePreview: "System online",
      messageCount: 2,
      updatedAt: pastTimestamp(1000),
    })
  );

  const smRef = sessionRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    smRef.set({
      userId: ALICE_UID,
      sessionId: "session_1",
      role: "assistant",
      text: "Web grounding complete.",
      agent: "research",
      modelLabel: "gemini-3.8-flash+search",
      hasImage: false,
      webSources: ["https://vertexaisearch.cloud.google.com/grounding-api-redirect/sample"],
      webSourceTitles: ["firebase.google.com"],
      searchQueries: ["firebase ai logic google search grounding"],
      searchEntryPointHtml: "<div>Search suggestions</div>",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Reject oversized searchQueries (> 8)
  await assertFails(
    sessionRef.collection("messages").doc("msg_bad_queries").set({
      userId: ALICE_UID,
      sessionId: "session_1",
      role: "assistant",
      text: "Too many queries.",
      agent: "research",
      modelLabel: "gemini-3.8-flash+search",
      hasImage: false,
      webSources: [],
      searchQueries: Array.from({ length: 9 }, (_, i) => `q_${i}`),
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(sessionRef.collection("messages").get());
  await assertSucceeds(smRef.delete());
  await assertSucceeds(sessionRef.delete());

  // Plugin CRUD
  const plugRef = aliceDb.collection("users").doc(ALICE_UID).collection("plugins").doc("custom_1");
  await assertSucceeds(
    plugRef.set({
      userId: ALICE_UID,
      pluginKey: "kotlin_architect",
      name: "Kotlin Architect",
      description: "Enforces clean MVVM Kotlin responses",
      category: "Engineering",
      enabled: false,
      systemInstruction: "Prefer immutable data classes and StateFlow.",
      isBuiltIn: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(plugRef.update({ enabled: true, updatedAt: pastTimestamp(1000) }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("plugins").get());
  await assertSucceeds(plugRef.delete());

  // BrainRun create/list/delete
  const brRef = aliceDb.collection("users").doc(ALICE_UID).collection("brain_runs").doc("br_1");
  await assertSucceeds(
    brRef.set({
      userId: ALICE_UID,
      mode: "consolidator",
      prompt: "Synthesize project stack",
      summary: "Zaythar uses Compose + Firestore + Gemini.",
      confidence: 95,
      insights: ["Modular MVVM", "Zero-trust Firestore rules"],
      suggestedTriplesCount: 3,
      modelUsed: "gemini-3.8-flash",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("brain_runs").get());
  await assertSucceeds(brRef.delete());

  // Memory note CRUD + length guard
  const memRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1");
  await assertSucceeds(
    memRef.set({
      userId: ALICE_UID,
      content: "User prefers dark cybernetic theme.",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(memRef.update({ content: "User prefers dark crimson cybernetic theme.", updatedAt: pastTimestamp(1000) }));
  await assertFails(memRef.update({ content: "", updatedAt: pastTimestamp(500) }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertSucceeds(memRef.delete());

  // Knowledge fact CRUD + bounds guard
  const factRef = aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").doc("fact_1");
  await assertSucceeds(
    factRef.set({
      userId: ALICE_UID,
      subject: "Zaythar AI",
      predicate: "uses",
      objectValue: "Firebase AI Logic",
      source: "operator",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(factRef.update({ objectValue: "Firebase AI Logic + Firestore", updatedAt: pastTimestamp(1000) }));
  await assertFails(factRef.update({ subject: "", updatedAt: pastTimestamp(500) }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").get());
  await assertSucceeds(factRef.delete());
});
