import firebase_admin
from firebase_admin import credentials, firestore

# Step 1: Initialize Firebase Admin
cred = credentials.Certificate("firebase-key.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

# Step 2: Standard base32 geohash (matches Android GeoFireUtils, precision 10).
# Inlined so this script needs no extra pip package and is guaranteed to produce
# the same hashes the app writes on create.
_BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"


def encode_geohash(latitude, longitude, precision=10):
    lat_interval = [-90.0, 90.0]
    lon_interval = [-180.0, 180.0]
    geohash = []
    bits = [16, 8, 4, 2, 1]
    bit = 0
    ch = 0
    even = True
    while len(geohash) < precision:
        if even:
            mid = (lon_interval[0] + lon_interval[1]) / 2
            if longitude > mid:
                ch |= bits[bit]
                lon_interval[0] = mid
            else:
                lon_interval[1] = mid
        else:
            mid = (lat_interval[0] + lat_interval[1]) / 2
            if latitude > mid:
                ch |= bits[bit]
                lat_interval[0] = mid
            else:
                lat_interval[1] = mid
        even = not even
        if bit < 4:
            bit += 1
        else:
            geohash.append(_BASE32[ch])
            bit = 0
            ch = 0
    return "".join(geohash)


# Step 3: Backfill a geohash onto every located post that lacks one.
# Idempotent: posts without location are skipped; posts are overwritten with the
# deterministic hash, so re-running is safe.
posts_ref = db.collection("posts")
updated = 0
skipped_no_location = 0

for post in posts_ref.stream():
    data = post.to_dict()
    lat = data.get("latitude")
    lng = data.get("longitude")

    if lat is None or lng is None:
        skipped_no_location += 1
        continue

    geohash = encode_geohash(float(lat), float(lng))
    posts_ref.document(post.id).update({"geohash": geohash})
    updated += 1
    print(f"Updated post {post.id}: geohash {geohash}")

print(f"\nDone. Updated {updated}, skipped {skipped_no_location} (no location).")
