import firebase_admin
from firebase_admin import credentials, firestore

# One-time migration: remove the `email` field from every users/{uid} document.
#
# Why: profile docs are world-readable to any signed-in user (including guests),
# but the app never reads UserProfile.email — the owner's email always comes from
# FirebaseAuth. Storing it in the public profile leaked every user's email. The
# field was removed from the client; this strips it from existing docs so old
# data isn't left readable.
#
# Idempotent: docs without an `email` field are skipped, so re-running is safe.
# Run from this folder with the service-account key present (scripts/firebase-key.json).

cred = credentials.Certificate("firebase-key.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

users_ref = db.collection("users")
updated = 0
skipped = 0

for user in users_ref.stream():
    data = user.to_dict()
    if "email" not in data:
        skipped += 1
        continue

    users_ref.document(user.id).update({"email": firestore.DELETE_FIELD})
    updated += 1
    print(f"Stripped email from users/{user.id}")

print(f"\nDone. Stripped {updated}, skipped {skipped} (no email field).")
