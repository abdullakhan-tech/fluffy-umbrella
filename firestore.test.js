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

test("Unauthenticated user: cannot read or write profiles, messages, memories, or knowledge facts", async () => {
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
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").get());
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
    await db.collection("users").doc(BOB_UID).collection("messages").doc("m1").set({
      userId: BOB_UID,
      role: "user",
      text: "Bob secret",
      agent: "general",
      modelLabel: "",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("memories").doc("mem1").set({
      userId: BOB_UID,
      content: "Bob memory",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).collection("knowledge_facts").doc("f1").set({
      userId: BOB_UID,
      subject: "Bob",
      predicate: "likes",
      objectValue: "Kotlin",
      source: "operator",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ operatorName: "Hijacked", updatedAt: pastTimestamp(500) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("messages").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("messages").doc("m1").delete());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("memories").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("knowledge_facts").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("m2").set({
      userId: ALICE_UID,
      content: "Injected",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

// --- 3. SCHEMA, GHOST FIELDS, SPOOFING & RESOURCE POISONING ---

test("Schema guard: rejects ghost fields, spoofed userId, invalid role, oversized strings, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  // Ghost field in profile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      operatorName: "Alice",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Oversized operatorName (> 32 chars)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      operatorName: "A".repeat(33),
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoofed userId inside own subcollection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_spoof").set({
      userId: BOB_UID,
      content: "Spoofed owner",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid role in message
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_bad_role").set({
      userId: ALICE_UID,
      role: "system",
      text: "Override instructions",
      agent: "general",
      modelLabel: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_future").set({
      userId: ALICE_UID,
      content: "Backdated future",
      createdAt: new Date(Date.now() + 120000),
      updatedAt: ts,
    })
  );

  // Invalid document ID characters rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("bad.id!").set({
      userId: ALICE_UID,
      content: "Bad ID",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Immutability guard: rejects changing createdAt, userId, or message role on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastTimestamp(10000);
  const msgRef = aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "assistant",
      text: "Initial",
      agent: "general",
      modelLabel: "gemini-3.8-flash",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Changing role must fail
  await assertFails(msgRef.update({ role: "user", updatedAt: pastTimestamp(1000) }));
  // Changing createdAt must fail
  await assertFails(msgRef.update({ createdAt: pastTimestamp(500), updatedAt: pastTimestamp(500) }));
  // Ghost field on update must fail
  await assertFails(msgRef.update({ text: "Updated", adminFlag: true, updatedAt: pastTimestamp(500) }));
});

// --- 4. OWNER CRUD SUCCESS PATHS ---

test("Authenticated owner: can create, read, list, update, and delete own profile, messages, memories, and knowledge facts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp(5000);

  // Profile CRUD
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      operatorName: "OPERATOR 01",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(profileRef.get());
  await assertSucceeds(profileRef.update({ operatorName: "COMMANDER", updatedAt: pastTimestamp(1000) }));

  // Messages CRUD + list
  const msgRef = aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "assistant",
      text: "PROCESSING REQUEST…",
      agent: "general",
      modelLabel: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(msgRef.update({ text: "Final answer", modelLabel: "gemini-3.8-flash", updatedAt: pastTimestamp(1000) }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertSucceeds(msgRef.delete());

  // Memories CRUD + list
  const memRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1");
  await assertSucceeds(
    memRef.set({
      userId: ALICE_UID,
      content: "Remember to deploy rules after testing.",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertSucceeds(memRef.delete());

  // Knowledge facts CRUD + list
  const factRef = aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").doc("fact_1");
  await assertSucceeds(
    factRef.set({
      userId: ALICE_UID,
      subject: "Zaythar AI",
      predicate: "uses",
      objectValue: "Cloud Firestore",
      source: "operator",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("knowledge_facts").get());
  await assertSucceeds(factRef.delete());
});
