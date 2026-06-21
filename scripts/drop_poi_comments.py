"""One-off cleanup: delete the legacy `pois/{poiId}/comments` subcollections.

POIs are now threads of posts (see docs/poi_documentation.md §4), so the old POI
comment documents are dead data. This removes them. Safe to run more than once.

Run from the scripts/ folder so it finds the service-account key:
    cd scripts && python drop_poi_comments.py
"""

import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate("firebase-key.json")  # gitignored; place in scripts/
firebase_admin.initialize_app(cred)
db = firestore.client()

BATCH = 300


def delete_collection(coll_ref):
    """Delete every doc in a (sub)collection in batches. Returns the count."""
    deleted = 0
    while True:
        docs = list(coll_ref.limit(BATCH).stream())
        if not docs:
            break
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
        batch.commit()
        deleted += len(docs)
        if len(docs) < BATCH:
            break
    return deleted


def main():
    total = 0
    pois = list(db.collection("pois").stream())
    for poi in pois:
        comments = poi.reference.collection("comments")
        removed = delete_collection(comments)
        if removed:
            print(f"  {poi.id}: removed {removed} comment(s)")
        total += removed
    print(f"✅ Done. Removed {total} POI comment(s) across {len(pois)} POI(s).")


if __name__ == "__main__":
    main()
