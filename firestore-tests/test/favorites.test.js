// Security-rules tests for the `favorites` map clause in ../../firestore.rules.
//
// Run via `npm run emulate` (boots the Firestore emulator, then mocha). These
// prove the rule actually does what the favorite-POIs feature assumes: a cap of
// 3, additions stamped to request.time (no backdating), immutable existing
// slots, and a 7-day removal hold.

const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const {
  doc,
  setDoc,
  updateDoc,
  serverTimestamp,
  deleteField,
  Timestamp,
  setLogLevel,
} = require('firebase/firestore');

const DAY = 24 * 60 * 60 * 1000;
const ALICE = 'alice';

let testEnv;

before(async () => {
  setLogLevel('error'); // hush the expected permission-denied noise
  testEnv = await initializeTestEnvironment({
    projectId: 'echo-rules-test',
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../../firestore.rules'), 'utf8'),
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

// Seed a user profile (optionally with favorites) bypassing the rules.
async function seedUser(uid, favorites) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const data = { uid, username: uid, firstName: 'A', lastName: 'B', bio: '' };
    if (favorites) data.favorites = favorites;
    await setDoc(doc(ctx.firestore(), 'users', uid), data);
  });
}

// Alice's Firestore handle, authenticated as a real (non-anonymous) account.
function aliceDb() {
  return testEnv.authenticatedContext(ALICE).firestore();
}

describe('favorites map rules', () => {
  it('adds a first favorite stamped with the server time', async () => {
    await seedUser(ALICE);
    await assertSucceeds(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi1': serverTimestamp() })
    );
  });

  it('rejects a backdated favorite timestamp', async () => {
    await seedUser(ALICE);
    await assertFails(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi1': Timestamp.fromMillis(1000) })
    );
  });

  it('allows a 3rd favorite when two slots are used', async () => {
    const old = Timestamp.fromMillis(Date.now() - 30 * DAY);
    await seedUser(ALICE, { poi1: old, poi2: old });
    await assertSucceeds(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi3': serverTimestamp() })
    );
  });

  it('rejects a 4th favorite (cap is 3)', async () => {
    const old = Timestamp.fromMillis(Date.now() - 30 * DAY);
    await seedUser(ALICE, { poi1: old, poi2: old, poi3: old });
    await assertFails(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi4': serverTimestamp() })
    );
  });

  it('rejects removing a favorite before the 7-day hold elapses', async () => {
    await seedUser(ALICE, { poi1: Timestamp.fromMillis(Date.now() - 2 * DAY) });
    await assertFails(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi1': deleteField() })
    );
  });

  it('allows removing a favorite after the 7-day hold elapses', async () => {
    await seedUser(ALICE, { poi1: Timestamp.fromMillis(Date.now() - 8 * DAY) });
    await assertSucceeds(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi1': deleteField() })
    );
  });

  it('rejects mutating an existing slot timestamp', async () => {
    await seedUser(ALICE, { poi1: Timestamp.fromMillis(Date.now() - 30 * DAY) });
    await assertFails(
      updateDoc(doc(aliceDb(), 'users', ALICE), { 'favorites.poi1': serverTimestamp() })
    );
  });

  it("rejects favoriting on someone else's profile", async () => {
    await seedUser('bob');
    await assertFails(
      updateDoc(doc(aliceDb(), 'users', 'bob'), { 'favorites.poi1': serverTimestamp() })
    );
  });
});
