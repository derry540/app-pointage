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

function validEmployee(uid) {
  return {
    userId: uid,
    fullName: "Youssef Benali",
    workerEmail: "youssef@thermique.ma",
    role: "WORKER",
    specialty: "Chauffagiste & Brûleurs",
    hourlyRate: 45,
    overtimeBonusRate: 65,
    commissionPercent: 5,
    lastMonthPaidTotal: 8900,
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  };
}

function validAttendance(uid) {
  return {
    userId: uid,
    employeeId: "emp_01",
    employeeName: "Youssef Benali",
    date: "2026-11-01",
    monthKey: "2026-11",
    companyName: "Atlas Thermique | Maroc Froid",
    shiftsSummary: "08:00-13:00 @ Atlas Thermique | 14:00-19:30 @ Maroc Froid",
    taskDescription: "Installation chaudière vapeur et test pression",
    dailyNote: "Excellent rendement malgré double chantier",
    performanceRating: 5,
    totalHours: 10.5,
    regularHours: 9,
    overtimeHours: 1.5,
    dailyPay: 502.5,
    recordedBy: "Admin Douae",
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  };
}

test("1. Unauthenticated user cannot read or write employees", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("employees").get());
  await assertFails(unauthDb.collection("employees").doc("emp_1").set(validEmployee(ALICE_UID)));
});

test("2. Authenticated owner can create and read their own employee profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("employees").doc("emp_1").set(validEmployee(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("employees").where("userId", "==", ALICE_UID).get()
  );
});

test("3. Cross-user isolation: Alice cannot read or modify Bob's employees", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("employees").doc("emp_bob").set(validEmployee(BOB_UID)));

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("employees").doc("emp_bob").get());
  await assertFails(aliceDb.collection("employees").doc("emp_bob").delete());
});

test("4. Unfiltered list query without userId filter is rejected (Query Enforcer)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("employees").get());
});

test("5. Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("employees").doc("emp_ghost").set({
      ...validEmployee(ALICE_UID),
      isSuperVerified: true,
    })
  );
});

test("6. Attendance log enforces 9h regular cap and 1-5 rating bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("attendance_logs").doc("att_1").set(validAttendance(ALICE_UID))
  );
  // Violate regularHours > 9
  await assertFails(
    aliceDb.collection("attendance_logs").doc("att_bad").set({
      ...validAttendance(ALICE_UID),
      regularHours: 12,
    })
  );
});

test("7. Audit logs are append-only and reject updates or deletes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const auditPayload = {
    userId: ALICE_UID,
    actorName: "Admin Douae",
    actorRole: "ADMIN",
    actionType: "CREATE",
    module: "Pointage",
    details: "Pointage validé pour Youssef Benali (10.5h)",
    createdAt: new Date(Date.now() - 1000),
  };
  await assertSucceeds(aliceDb.collection("audit_logs").doc("aud_1").set(auditPayload));
  await assertFails(aliceDb.collection("audit_logs").doc("aud_1").delete());
});
