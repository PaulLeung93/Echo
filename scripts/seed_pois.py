import json
import sys
import urllib.parse
import urllib.request

# The Windows console defaults to cp1252, which can't encode the status emoji
# below; force UTF-8 so the script prints cleanly everywhere.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime

# Step 1: Authenticate with Firebase
cred = credentials.Certificate("firebase-key.json")  # place the service-account key in scripts/ (gitignored); run from this folder
firebase_admin.initialize_app(cred)
db = firestore.client()

# Curated photos are resolved from Wikipedia/Wikimedia Commons at seed time:
# free to hotlink, clearly licensed, and no hosting on our side. We ask the
# MediaWiki "pageimages" API for each place's lead image as a fixed-width
# thumbnail, which returns a stable upload.wikimedia.org URL (and follows
# redirects, so e.g. "Battery Park" resolves to "The Battery (Manhattan)").
WIKI_THUMB_WIDTH = 800
# Wikimedia's API policy requires a descriptive User-Agent with contact info.
WIKI_USER_AGENT = "EchoApp/1.0 (POI seeding; paul.leung@codepath.org)"


def commons_file_url(filename):
    """Build a stable, fixed-width thumbnail URL for a Commons file."""
    name = urllib.parse.quote(filename)
    return (f"https://commons.wikimedia.org/wiki/Special:FilePath/{name}"
            f"?width={WIKI_THUMB_WIDTH}")


def fetch_wikimedia_image(title):
    """Return a stable thumbnail URL for the article's lead image, or None."""
    params = urllib.parse.urlencode({
        "action": "query",
        "format": "json",
        "prop": "pageimages",
        "piprop": "thumbnail",
        "pithumbsize": WIKI_THUMB_WIDTH,
        "redirects": 1,
        "titles": title,
    })
    url = f"https://en.wikipedia.org/w/api.php?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": WIKI_USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.load(resp)
        pages = data.get("query", {}).get("pages", {})
        for page in pages.values():
            thumb = page.get("thumbnail", {}).get("source")
            if thumb:
                return thumb
    except Exception as e:  # network/parse errors → fall back to the type icon
        print(f"   ⚠️  image lookup failed for '{title}': {e}")
    return None

# Step 2: Define your POIs
pois = [
    # Colleges
    {
        "name": "Barnard College",
        "type": "college",
        "location": firestore.GeoPoint(40.8090, -73.9631),
        "description": "Women's liberal arts college affiliated with Columbia University.",
        # Page lead image is a non-free seal; use a free Commons campus photo.
        "image_file": "Barnard College of Columbia University (52008382627).jpg"
    },
    {
        "name": "City College of New York",
        "type": "college",
        "location": firestore.GeoPoint(40.8200, -73.9496),
        "description": "Public college in Hamilton Heights, Manhattan."
    },
    {
        "name": "Columbia University",
        "type": "college",
        "location": firestore.GeoPoint(40.807536, -73.962573),
        "description": "Ivy League university in NYC."
    },
    {
        "name": "Cooper Union",
        "type": "college",
        "location": firestore.GeoPoint(40.7291, -73.9906),
        "description": "Private college known for architecture and engineering."
    },
    {
        "name": "Fordham University",
        "type": "college",
        "location": firestore.GeoPoint(40.8612, -73.8860),
        "description": "Private university with a campus in the Bronx.",
        "image_file": "Keating Hall, Fordham University Rose Hill.jpg"
    },
    {
        "name": "Juilliard School",
        "type": "college",
        "location": firestore.GeoPoint(40.7738, -73.9830),
        "description": "Renowned performing arts conservatory at Lincoln Center.",
        "image_file": "Juilliard School.jpg"
    },
    {
        "name": "New York University",
        "type": "college",
        "location": firestore.GeoPoint(40.729513, -73.996461),
        "description": "Private research university based in Manhattan."
    },
    {
        "name": "Pratt Institute",
        "type": "college",
        "location": firestore.GeoPoint(40.6914, -73.9635),
        "description": "Brooklyn-based college focused on art, design, and architecture.",
        "image_file": "Pratt Institute.jpg"
    },
    {
        "name": "Queens College",
        "type": "college",
        "location": firestore.GeoPoint(40.7365, -73.8203),
        "description": "Public college in Flushing, Queens.",
        "wiki": "Queens College, City University of New York"
    },
    {
        "name": "St. John's University",
        "type": "college",
        "location": firestore.GeoPoint(40.7218, -73.7957),
        "description": "Private Catholic university located in Queens.",
        "wiki": "St. John's University (New York City)"
    },
    {
        "name": "The New School",
        "type": "college",
        "location": firestore.GeoPoint(40.7352, -73.9946),
        "description": "Progressive private university near Union Square."
    },
    {
        "name": "Brooklyn College",
        "type": "college",
        "location": firestore.GeoPoint(40.6308, -73.9520),
        "description": "Public college in Flatbush, Brooklyn."
    },
    

    # Parks
    {
        "name": "Battery Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7033, -74.0170),
        "description": "Waterfront park at the southern tip of Manhattan."
    },
    {
        "name": "Bryant Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7536, -73.9832),
        "description": "Public park located in Midtown Manhattan."
    },
    {
        "name": "Central Park",
        "type": "park",
        "location": firestore.GeoPoint(40.78300554128858, -73.96529372466715),
        "description": "An iconic urban park in Manhattan."
    },
    {
        "name": "Flushing Meadows-Corona Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7498, -73.8408),
        "description": "Major park in Queens, home to the Unisphere."
    },
    {
        "name": "Fort Greene Park",
        "type": "park",
        "location": firestore.GeoPoint(40.6924, -73.9744),
        "description": "Historic park in Brooklyn, site of a Revolutionary War fort."
    },
    {
        "name": "High Line",
        "type": "park",
        "location": firestore.GeoPoint(40.7479, -74.0049),
        "description": "Elevated linear park built on a historic freight rail line."
    },
    {
        "name": "Hudson River Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7390, -74.0102),
        "description": "Expansive waterfront park with recreational piers and greenways."
    },
    {
        "name": "Madison Square Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7425, -73.9880),
        "description": "Lush park near the Flatiron Building."
    },
    {
        "name": "Pelham Bay Park",
        "type": "park",
        "location": firestore.GeoPoint(40.8732, -73.8057),
        "description": "The largest park in NYC, located in the Bronx."
    },
    {
        "name": "Prospect Park",
        "type": "park",
        "location": firestore.GeoPoint(40.6602, -73.9690),
        "description": "Large public park in Brooklyn.",
        "wiki": "Prospect Park (Brooklyn)"
    },
    {
        "name": "Riverside Park",
        "type": "park",
        "location": firestore.GeoPoint(40.8007, -73.9707),
        "description": "Scenic park along the Hudson River with bike paths and playgrounds.",
        "wiki": "Riverside Park (Manhattan)"
    },
    {
        "name": "Tompkins Square Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7265, -73.9815),
        "description": "East Village park known for community events and activism."
    },
    {
        "name": "Union Square",
        "type": "park",
        "location": firestore.GeoPoint(40.7359, -73.9911),
        "description": "Historic intersection and gathering place with a famous greenmarket.",
        "wiki": "Union Square, Manhattan"
    },
    {
        "name": "Washington Market Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7175, -74.0107),
        "description": "Neighborhood park in Tribeca with play areas and gardens."
    },
    {
        "name": "Washington Square Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7308, -73.9973),
        "description": "Famous park in Greenwich Village."
    },
    

    # Landmarks
    {
        "name": "Brooklyn Bridge",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7061, -73.9969),
        "description": "Historic bridge connecting Manhattan and Brooklyn."
    },
    {
        "name": "Chrysler Building",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7516, -73.9755),
        "description": "Art Deco skyscraper and staple of the NYC skyline.",
        "image_file": "Chrysler Building by David Shankbone Retouched.jpg"
    },
    {
        "name": "Empire State Building",
        "type": "landmark",
        "location": firestore.GeoPoint(40.748817, -73.985428),
        "description": "Iconic skyscraper offering panoramic city views."
    },
    {
        "name": "Flatiron Building",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7411, -73.9897),
        "description": "Iconic triangular building at the intersection of Fifth Ave and Broadway."
    },
    {
        "name": "Grand Central Terminal",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7527, -73.9772),
        "description": "Historic train terminal with stunning architecture and a celestial ceiling."
    },
    {
        "name": "Lincoln Center",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7730, -73.9822),
        "description": "Premier performing arts center."
    },
    {
        "name": "One World Trade Center",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7127, -74.0134),
        "description": "Tallest building in the Western Hemisphere."
    },
    {
        "name": "Rockefeller Center",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7587, -73.9787),
        "description": "Home of the NYC Christmas tree, Top of the Rock, and NBC Studios."
    },
    {
        "name": "Statue of Liberty",
        "type": "landmark",
        "location": firestore.GeoPoint(40.689247, -74.044502),
        "description": "Symbol of freedom in the United States."
    },
    {
        "name": "The Vessel",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7538, -74.0021),
        "description": "Modern interactive structure at Hudson Yards.",
        "wiki": "Vessel (structure)"
    },
    {
        "name": "Times Square",
        "type": "landmark",
        "location": firestore.GeoPoint(40.758896, -73.985130),
        "description": "Famous intersection known for its lights and Broadway theaters."
    }

]

# Step 3: Clear existing POIs to avoid duplicates
print("🗑️ Clearing existing POIs...")
docs = db.collection("pois").stream()
for doc in docs:
    doc.reference.delete()

# Step 4: Resolve a curated photo for each POI, then upload to Firestore
print("📷 Resolving POI photos from Wikimedia…")
with_photo = 0
for poi in pois:
    # "image_file" pins a hand-picked free Commons photo (used when the page's
    # lead image is a non-free logo); otherwise resolve the lead image by title.
    # "wiki" is an optional title override (disambiguation); default to the name.
    image_file = poi.pop("image_file", None)
    title = poi.pop("wiki", poi["name"])
    image_url = commons_file_url(image_file) if image_file else fetch_wikimedia_image(title)
    poi["imageUrl"] = image_url or ""  # blank → the app shows the type-icon fallback
    if image_url:
        with_photo += 1
        print(f"   ✓ {poi['name']}")
    else:
        print(f"   – {poi['name']} (no photo; type-icon fallback)")
    poi["createdAt"] = datetime.utcnow()
    db.collection("pois").add(poi)

print(f"✅ Successfully uploaded {len(pois)} POIs to Firestore "
      f"({with_photo} with photos, {len(pois) - with_photo} fallback).")
