"use strict";

/**
 * Echo Cloud Functions.
 *
 * onPostDeleted: when a post is deleted, (1) cascade-delete its `comments`
 * subcollection (Firestore does not delete subcollections with a document), and
 * (2) if the post belonged to a POI thread, decrement that POI's denormalized
 * `postCount`. Runs with admin privileges, so it bypasses security rules.
 */

const { onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

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
