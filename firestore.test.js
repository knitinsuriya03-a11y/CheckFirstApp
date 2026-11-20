const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_checkfirst_123";
const BOB_UID = "bob_checkfirst_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validPrefsPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    tamil: false,
    simpleMode: false,
    largeText: false,
    safetyTips: true,
    completionAlerts: false,
    saveHistory: false,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validHistoryPayload(uid, overrides = {}) {
  return {
    userId: uid,
    kind: "Message scan",
    label: "Message scan result",
    risk: "danger",
    createdAt: new Date(Date.now() - 2000),
    ...overrides,
  };
}

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/preferences/default`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/preferences/default`).set(validPrefsPayload(ALICE_UID)));
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/checkHistory`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/checkHistory/entry_1`).set(validHistoryPayload(ALICE_UID)));
});

test("Authenticated owner: valid preferences create, partial update, and read succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb.doc(`users/${ALICE_UID}/preferences/default`);

  await assertSucceeds(prefRef.set(validPrefsPayload(ALICE_UID)));
  await assertSucceeds(prefRef.get());
  await assertSucceeds(
    prefRef.update({
      tamil: true,
      simpleMode: true,
      saveHistory: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Preferences security bounds: reject ghost fields, immutable mutation, wrong docId, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb.doc(`users/${ALICE_UID}/preferences/default`);

  // Wrong document ID
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/preferences/other`).set(validPrefsPayload(ALICE_UID)));

  // Ghost field on create
  await assertFails(prefRef.set(validPrefsPayload(ALICE_UID, { isAdmin: true })));

  // Future timestamp rejected
  await assertFails(
    prefRef.set(validPrefsPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Create valid doc then test immutable field tampering
  await assertSucceeds(prefRef.set(validPrefsPayload(ALICE_UID)));
  await assertFails(
    prefRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    prefRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Authenticated owner: valid checkHistory create, ordered list query, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entryRef = aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_1`);

  // Mirrors Kotlin repository create payload (no @DocumentId `id` key)
  await assertSucceeds(entryRef.set(validHistoryPayload(ALICE_UID)));
  await assertSucceeds(entryRef.get());
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/checkHistory`).orderBy("createdAt", "desc").limit(30).get()
  );

  // Updates to immutable audit history entries must fail
  await assertFails(entryRef.update({ risk: "clear" }));

  // Owner can clear/delete history entries
  await assertSucceeds(entryRef.delete());
});

test("CheckHistory validation: reject @DocumentId id field, invalid risk enum, oversized strings, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Reject unexpected `id` field in body
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_id`).set(validHistoryPayload(ALICE_UID, { id: "entry_id" }))
  );

  // Reject invalid risk enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_bad_risk`).set(validHistoryPayload(ALICE_UID, { risk: "CRITICAL" }))
  );

  // Reject oversized label (>120 chars)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_big`).set(validHistoryPayload(ALICE_UID, { label: "x".repeat(121) }))
  );

  // Reject userId spoofing
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_spoof`).set(validHistoryPayload(BOB_UID))
  );

  // Cross-user isolation
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/checkHistory/entry_alice`).set(validHistoryPayload(ALICE_UID)));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/checkHistory/entry_alice`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/checkHistory`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/checkHistory/entry_alice`).delete());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/preferences/default`).get());
});
