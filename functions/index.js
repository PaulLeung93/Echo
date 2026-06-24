"use strict";

/**
 * Echo Cloud Functions.
 *
 * onPostDeleted: when a post is deleted, (1) cascade-delete its `comments`
 * subcollection (Firestore does not delete subcollections with a document), and
 * (2) if the post belonged to a POI thread, decrement that POI's denormalized
 * `postCount`.
 *
 * onUserDeleted: when a user's profile doc is deleted (the last Firestore step of
 * account deletion), cascade-delete the content they would otherwise leave behind:
 * their posts, their comments on other people's posts, and their avatar in Storage.
 * Without this, a deleted user's posts keep showing in the feed/map owned by a uid
 * that no longer exists.
 *
 * onFollowCreated / onFollowDeleted: maintain the follow graph. The client writes
 * only its own `users/{uid}/following/{target}` edge; these mirror the `followers`
 * entry onto the target and keep the denormalized follower/following counts in sync.
 *
 * onUserBlocked: when a user adds someone to their blockedUserIds, sever the follow
 * relationship both ways (block is authoritative server-side).
 *
 * All run with admin privileges, so they bypass security rules.
 */

const {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");

initializeApp();
const db = getFirestore();

exports.onPostDeleted = onDocumentDeleted("posts/{postId}", async (event) => {
  const snap = event.data;
  if (!snap) return;

  const post = snap.data() || {};
  const postId = event.params.postId;

  // 1) Cascade-delete the post's comments subcollection.
  const commentsRef = db.collection("posts").doc(postId).collection("comments");
  await deleteCollection(commentsRef, 300);

  // 2) If this was a POI post, keep the POI's denormalized postCount in sync.
  const poiId = post.poiId;
  if (poiId) {
    try {
      await db
        .collection("pois")
        .doc(poiId)
        .update({ postCount: FieldValue.increment(-1) });
    } catch (e) {
      // The POI may have been removed out-of-band; log and move on.
      console.error(`Failed to decrement postCount for POI ${poiId}`, e);
    }
  }
});

exports.onUserDeleted = onDocumentDeleted("users/{uid}", async (event) => {
  const uid = event.params.uid;

  // 1) Delete the user's own posts. Deleting each post fires onPostDeleted, which
  //    cascades that post's comments and fixes the POI postCount — so we don't
  //    repeat that work here. Individual deletes (not a batch) so this scales past
  //    the 500-op batch limit for prolific authors.
  const ownPostsSnap = await db
    .collection("posts")
    .where("authorId", "==", uid)
    .get();
  const ownPostIds = new Set();
  await Promise.all(
    ownPostsSnap.docs.map((doc) => {
      ownPostIds.add(doc.id);
      return doc.ref.delete();
    })
  );

  // 2) Delete the user's comments on *other people's* posts. (Comments on their
  //    own posts are removed by the onPostDeleted cascade above, so we skip those.)
  //    Group by parent post so each post's denormalized `commentCount` is corrected
  //    in step with the deletes.
  const commentsSnap = await db
    .collectionGroup("comments")
    .where("authorId", "==", uid)
    .get();

  const byPost = new Map(); // postId -> { postRef, commentRefs: [] }
  for (const doc of commentsSnap.docs) {
    const postRef = doc.ref.parent.parent; // posts/{postId}
    if (!postRef || ownPostIds.has(postRef.id)) continue; // own posts: cascaded
    const entry = byPost.get(postRef.id) || { postRef, commentRefs: [] };
    entry.commentRefs.push(doc.ref);
    byPost.set(postRef.id, entry);
  }

  for (const { postRef, commentRefs } of byPost.values()) {
    try {
      // Chunk well under the 500-op batch limit (each chunk: N deletes + 1 update).
      for (let i = 0; i < commentRefs.length; i += 400) {
        const chunk = commentRefs.slice(i, i + 400);
        const batch = db.batch();
        chunk.forEach((ref) => batch.delete(ref));
        batch.update(postRef, {
          commentCount: FieldValue.increment(-chunk.length),
        });
        await batch.commit();
      }
    } catch (e) {
      // The post may have been deleted concurrently (its comments go with it via
      // onPostDeleted); log and move on rather than failing the whole cleanup.
      console.error(
        `Failed to remove ${commentRefs.length} comments on post ${postRef.id}`,
        e
      );
    }
  }

  // 3) Best-effort: delete the user's avatar from Storage. A missing object
  //    (they never set one) is expected and must not fail the cleanup.
  try {
    await getStorage().bucket().file(`avatars/${uid}.jpg`).delete();
  } catch (e) {
    if (e.code !== 404) {
      console.error(`Failed to delete avatar for ${uid}`, e);
    }
  }

  // 4) Follow graph. Subcollections survive their parent doc's deletion, so the
  //    deleted user's `following`/`followers` must be cleaned explicitly. Deleting
  //    each edge fires onFollowDeleted, which removes the mirror and fixes the
  //    *other* side's count (updates to the now-deleted user's own doc no-op).
  //    (a) Edges this user created (they follow X): fixes each X.followerCount.
  const followingSnap = await db.collection(`users/${uid}/following`).get();
  await Promise.all(followingSnap.docs.map((d) => d.ref.delete()));
  //    (b) Edges others created toward this user (Y follows them): delete the
  //        source-of-truth doc users/Y/following/{uid}; fixes each Y.followingCount
  //        and removes this user's follower mirror.
  const followersSnap = await db.collection(`users/${uid}/followers`).get();
  await Promise.all(
    followersSnap.docs.map((d) =>
      db
        .doc(`users/${d.id}/following/${uid}`)
        .delete()
        .catch((e) =>
          console.error(`Failed to remove follow edge ${d.id} -> ${uid}`, e)
        )
    )
  );
  //    (c) Mop up any leftovers in this user's own subcollections (idempotent).
  await deleteCollection(db.collection(`users/${uid}/following`), 300);
  await deleteCollection(db.collection(`users/${uid}/followers`), 300);
});

// ----- Follow graph -----
//
// The client writes only its own `users/{uid}/following/{targetUid}` edge. These
// functions mirror the `followers` entry onto the target and keep the denormalized
// followerCount/followingCount in sync. They run with admin privileges, so they can
// write the count fields the security rules forbid clients from touching.

exports.onFollowCreated = onDocumentCreated(
  "users/{uid}/following/{targetUid}",
  async (event) => {
    const { uid, targetUid } = event.params;
    const createdAt =
      (event.data && event.data.data() && event.data.data().createdAt) ||
      FieldValue.serverTimestamp();

    // Mirror the edge onto the target's followers list.
    try {
      await db.doc(`users/${targetUid}/followers/${uid}`).set({ createdAt });
    } catch (e) {
      console.error(`Failed to mirror follower ${uid} -> ${targetUid}`, e);
    }

    // Bump both denormalized counts (best-effort; a missing doc must not throw).
    await Promise.all([
      db
        .doc(`users/${uid}`)
        .update({ followingCount: FieldValue.increment(1) })
        .catch((e) => console.error(`Failed to bump followingCount for ${uid}`, e)),
      db
        .doc(`users/${targetUid}`)
        .update({ followerCount: FieldValue.increment(1) })
        .catch((e) =>
          console.error(`Failed to bump followerCount for ${targetUid}`, e)
        ),
    ]);
  }
);

exports.onFollowDeleted = onDocumentDeleted(
  "users/{uid}/following/{targetUid}",
  async (event) => {
    const { uid, targetUid } = event.params;

    await Promise.all([
      db
        .doc(`users/${targetUid}/followers/${uid}`)
        .delete()
        .catch((e) =>
          console.error(`Failed to remove follower ${uid} -> ${targetUid}`, e)
        ),
      db
        .doc(`users/${uid}`)
        .update({ followingCount: FieldValue.increment(-1) })
        .catch((e) =>
          console.error(`Failed to drop followingCount for ${uid}`, e)
        ),
      db
        .doc(`users/${targetUid}`)
        .update({ followerCount: FieldValue.increment(-1) })
        .catch((e) =>
          console.error(`Failed to drop followerCount for ${targetUid}`, e)
        ),
    ]);
  }
);

// When a user blocks someone, sever the follow relationship both ways. Block is a
// single arrayUnion on the client; this makes it authoritative server-side. Deleting
// each `following` edge fires onFollowDeleted, which clears mirrors and counts.
exports.onUserBlocked = onDocumentUpdated("users/{uid}", async (event) => {
  const uid = event.params.uid;
  const before = (event.data.before.data() || {}).blockedUserIds || [];
  const after = (event.data.after.data() || {}).blockedUserIds || [];
  const newlyBlocked = after.filter((id) => !before.includes(id));
  if (newlyBlocked.length === 0) return;

  await Promise.all(
    newlyBlocked.flatMap((blockedUid) => [
      db
        .doc(`users/${uid}/following/${blockedUid}`)
        .delete()
        .catch((e) =>
          console.error(`Failed to unfollow ${blockedUid} for blocker ${uid}`, e)
        ),
      db
        .doc(`users/${blockedUid}/following/${uid}`)
        .delete()
        .catch((e) =>
          console.error(`Failed to remove ${blockedUid}'s follow of ${uid}`, e)
        ),
    ])
  );
});

/** Delete every document in a collection (or subcollection) in batches. */
async function deleteCollection(collectionRef, batchSize) {
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const snapshot = await collectionRef.limit(batchSize).get();
    if (snapshot.empty) break;

    const batch = collectionRef.firestore.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    if (snapshot.size < batchSize) break;
  }
}
