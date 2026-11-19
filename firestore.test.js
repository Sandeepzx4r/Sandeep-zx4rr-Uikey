const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_gamer_01";
const BOB_UID = "bob_gamer_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function makeUserProfile(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "BlackShark Player",
    playerCode: "BS123456",
    coinBalance: 0,
    totalEarnedCoins: 0,
    totalRedeemedCoins: 0,
    totalSessionsCompleted: 0,
    totalSpins: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("game_sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("spin_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("redemptions").get());
});

test("User can create profile starting at 0 coins, cannot inflate initial balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validProfile = makeUserProfile(ALICE_UID);

  // Inflated initial coins rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile,
      coinBalance: 5000,
    })
  );

  // Valid 0-coin profile succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));

  // Direct arbitrary coin update without atomic action rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 9999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Display name edit without touching balance succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "ProGamer",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("connected_games").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("redemptions").get());
});

test("Connected games + 1-hour game session real-time validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, now)));

  // Cannot start session before connecting game
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").doc("sess_unlinked").set({
      userId: ALICE_UID,
      gameKey: "pubg",
      gameTitle: "PUBG Mobile",
      status: "active",
      targetDurationSeconds: 3600,
      rewardCoins: 1000,
      startedAt: now,
      updatedAt: now,
    })
  );

  // Connect PUBG Mobile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("connected_games").doc("pubg").set({
      userId: ALICE_UID,
      gameKey: "pubg",
      gameTitle: "PUBG Mobile",
      inGamePlayerId: "5123456789",
      inGameUsername: "SharkHunter",
      connected: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Start session now
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").doc("sess_early").set({
      userId: ALICE_UID,
      gameKey: "pubg",
      gameTitle: "PUBG Mobile",
      status: "active",
      targetDurationSeconds: 3600,
      rewardCoins: 1000,
      startedAt: now,
      updatedAt: now,
    })
  );

  // Premature claim (started 2 seconds ago) MUST FAIL!
  const earlyBatch = aliceDb.batch();
  earlyBatch.update(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").doc("sess_early"),
    { status: "claimed", completedAt: new Date(Date.now() - 500), updatedAt: new Date(Date.now() - 500) }
  );
  earlyBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 1000,
    totalEarnedCoins: 1000,
    totalSessionsCompleted: 1,
    lastActionRefId: "sess_early",
    updatedAt: new Date(Date.now() - 500),
  });
  await assertFails(earlyBatch.commit());

  // Seed an active session started 61 minutes ago to verify valid real-time claim
  const overOneHourAgo = new Date(Date.now() - 3660 * 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore()
      .collection("users").doc(ALICE_UID)
      .collection("game_sessions").doc("sess_ready")
      .set({
        userId: ALICE_UID,
        gameKey: "pubg",
        gameTitle: "PUBG Mobile",
        status: "active",
        targetDurationSeconds: 3600,
        rewardCoins: 1000,
        startedAt: overOneHourAgo,
        updatedAt: overOneHourAgo,
      });
  });

  // Valid atomic claim after 1 hour succeeds
  const validClaimTime = new Date(Date.now() - 500);
  const validBatch = aliceDb.batch();
  validBatch.update(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").doc("sess_ready"),
    { status: "claimed", completedAt: validClaimTime, updatedAt: validClaimTime }
  );
  validBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 1000,
    totalEarnedCoins: 1000,
    totalSessionsCompleted: 1,
    lastActionRefId: "sess_ready",
    updatedAt: validClaimTime,
  });
  await assertSucceeds(validBatch.commit());

  // Double-claiming the same session MUST FAIL (terminal state lock)
  const doubleClaimBatch = aliceDb.batch();
  doubleClaimBatch.update(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").doc("sess_ready"),
    { status: "claimed", completedAt: new Date(Date.now() - 200), updatedAt: new Date(Date.now() - 200) }
  );
  doubleClaimBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 2000,
    totalEarnedCoins: 2000,
    totalSessionsCompleted: 2,
    lastActionRefId: "sess_ready",
    updatedAt: new Date(Date.now() - 200),
  });
  await assertFails(doubleClaimBatch.commit());
});

test("Spin Wheel atomic reward + cooldown + UPI/Google Play redemption validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, now)));

  // First Spin Wheel reward (+500 coins) in atomic batch succeeds
  const spinTime1 = new Date(Date.now() - 1000);
  const spinBatch1 = aliceDb.batch();
  spinBatch1.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("spin_logs").doc("spin_01"),
    {
      userId: ALICE_UID,
      rewardCoins: 500,
      segmentLabel: "500 Coins",
      createdAt: spinTime1,
    }
  );
  spinBatch1.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 500,
    totalEarnedCoins: 500,
    totalSpins: 1,
    lastSpinAt: spinTime1,
    lastActionRefId: "spin_01",
    updatedAt: spinTime1,
  });
  await assertSucceeds(spinBatch1.commit());

  // Immediate second spin within 60s cooldown MUST FAIL
  const spinTime2 = new Date(Date.now() - 500);
  const spinBatch2 = aliceDb.batch();
  spinBatch2.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("spin_logs").doc("spin_02"),
    {
      userId: ALICE_UID,
      rewardCoins: 500,
      segmentLabel: "500 Coins",
      createdAt: spinTime2,
    }
  );
  spinBatch2.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 1000,
    totalEarnedCoins: 1000,
    totalSpins: 2,
    lastSpinAt: spinTime2,
    lastActionRefId: "spin_02",
    updatedAt: spinTime2,
  });
  await assertFails(spinBatch2.commit());

  // Cannot redeem 1000 coins when balance is only 500
  const overRedeem = aliceDb.batch();
  overRedeem.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("redemptions").doc("red_over"),
    {
      userId: ALICE_UID,
      method: "upi",
      destination: "player@upi",
      coinsCost: 1000,
      amountInr: 1000,
      status: "submitted",
      referenceCode: "BSG-100001",
      createdAt: new Date(Date.now() - 400),
    }
  );
  overRedeem.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: -500,
    totalRedeemedCoins: 1000,
    lastActionRefId: "red_over",
    updatedAt: new Date(Date.now() - 400),
  });
  await assertFails(overRedeem.commit());

  // Valid 500-coin UPI redemption succeeds
  const validRedeem = aliceDb.batch();
  const redTime = new Date(Date.now() - 300);
  validRedeem.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("redemptions").doc("red_01"),
    {
      userId: ALICE_UID,
      method: "upi",
      destination: "player@upi",
      coinsCost: 500,
      amountInr: 500,
      status: "submitted",
      referenceCode: "BSG-500001",
      createdAt: redTime,
    }
  );
  validRedeem.update(aliceDb.collection("users").doc(ALICE_UID), {
    coinBalance: 0,
    totalRedeemedCoins: 500,
    lastActionRefId: "red_01",
    updatedAt: redTime,
  });
  await assertSucceeds(validRedeem.commit());

  // User-scoped list queries succeed
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("connected_games").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("game_sessions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("spin_logs").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("redemptions").get());
});
