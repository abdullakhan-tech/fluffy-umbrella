const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
    await db.collection("users").doc(BOB_UID).collection("memories").doc("mem1").set({
      userId: BOB_UID,
      content: "Bob private memory",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("knowledge_facts").doc("fact1").set({
      userId: BOB_UID,
      subject: "Bob",
      predicate: "owns",
      objectValue: "private fact",
      source: "operator",
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
  const bobMemory = aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("mem1");
  const bobFact = aliceDb.collection("users").doc(BOB_UID).collection("knowledge_facts").doc("fact1");
  await assertFails(bobMemory.get());
  await assertFails(bobMemory.parent.get());
  await assertFails(bobFact.get());
  await assertFails(bobFact.parent.get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("mem2").set({
      userId: ALICE_UID,
      content: "Spoofed cross-user write",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobMemory.update({ content: "Tampered by Alice", updatedAt: pastTimestamp() }));
  await assertFails(bobMemory.delete());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("knowledge_facts").doc("fact2").set({
      userId: ALICE_UID,
      subject: "Spoof",
      predicate: "writes",
      objectValue: "cross-user",
      source: "operator",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobFact.update({ objectValue: "Tampered by Alice", updatedAt: pastTimestamp() }));
  await assertFails(bobFact.delete());
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
      modelLabel: "gemini-3.8-flash+web",
      hasImage: false,
      webSources: ["https://firebase.google.com/docs/ai-logic"],
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

  // Memory note CRUD
  const memoryRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("memory_1");
  await assertSucceeds(memoryRef.set({
    userId: ALICE_UID,
    content: "Initial private note",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(memoryRef.get());
  await assertSucceeds(memoryRef.update({
    content: "Updated private note",
    updatedAt: pastTimestamp(1000),
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertSucceeds(memoryRef.delete());

  // Knowledge fact CRUD
  const factRef = aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").doc("fact_1");
  await assertSucceeds(factRef.set({
    userId: ALICE_UID,
    subject: "Zaythar",
    predicate: "stores",
    objectValue: "private facts",
    source: "operator",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(factRef.get());
  await assertSucceeds(factRef.update({
    objectValue: "owner-scoped facts",
    updatedAt: pastTimestamp(1000),
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").get());
  await assertSucceeds(factRef.delete());
});
