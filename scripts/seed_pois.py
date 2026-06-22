import json
import re
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


def slugify(name):
    """Stable doc ID from a POI name, e.g. 'Central Park' -> 'central-park'.
    Using a deterministic ID (instead of an auto-generated one) is what makes
    re-seeding idempotent: the same POI maps to the same document every run."""
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


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
    # --- Outer-borough colleges ---
    {
        "name": "NYU Tandon School of Engineering",
        "type": "college",
        "location": firestore.GeoPoint(40.6942, -73.9866),
        "description": "NYU's engineering school at MetroTech in Downtown Brooklyn.",
        "wiki": "New York University Tandon School of Engineering"
    },
    {
        "name": "LIU Brooklyn",
        "type": "college",
        "location": firestore.GeoPoint(40.6896, -73.9776),
        "description": "Long Island University's campus in Downtown Brooklyn.",
        "wiki": "Long Island University"
    },
    {
        "name": "York College",
        "type": "college",
        "location": firestore.GeoPoint(40.7003, -73.8005),
        "description": "CUNY senior college in Jamaica, Queens.",
        "wiki": "York College, City University of New York"
    },
    {
        "name": "LaGuardia Community College",
        "type": "college",
        "location": firestore.GeoPoint(40.7445, -73.9376),
        "description": "CUNY community college in Long Island City, Queens."
    },
    {
        "name": "Lehman College",
        "type": "college",
        "location": firestore.GeoPoint(40.8730, -73.8940),
        "description": "CUNY senior college in the Bronx."
    },
    {
        "name": "Manhattan College",
        "type": "college",
        "location": firestore.GeoPoint(40.8896, -73.8993),
        "description": "Private college in the Riverdale section of the Bronx."
    },
    {
        "name": "College of Staten Island",
        "type": "college",
        "location": firestore.GeoPoint(40.6018, -74.1502),
        "description": "CUNY senior college in Willowbrook, Staten Island."
    },
    {
        "name": "Wagner College",
        "type": "college",
        "location": firestore.GeoPoint(40.6149, -74.0935),
        "description": "Private college on Grymes Hill, Staten Island."
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
    # --- Outer-borough parks (major only) ---
    {
        "name": "Brooklyn Bridge Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7002, -73.9967),
        "description": "Waterfront park along Brooklyn's East River shoreline."
    },
    {
        "name": "Marine Park",
        "type": "park",
        "location": firestore.GeoPoint(40.5944, -73.9213),
        "description": "Brooklyn's largest park, with salt marsh and athletic fields.",
        "wiki": "Marine Park (Brooklyn)"
    },
    {
        "name": "Forest Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7016, -73.8569),
        "description": "Large wooded park spanning central Queens.",
        "wiki": "Forest Park (Queens)"
    },
    {
        "name": "Astoria Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7795, -73.9230),
        "description": "Riverside Queens park with NYC's largest pool and Hell Gate Bridge views."
    },
    {
        "name": "Alley Pond Park",
        "type": "park",
        "location": firestore.GeoPoint(40.7456, -73.7430),
        "description": "Second-largest park in Queens, with wetlands and trails."
    },
    {
        "name": "Van Cortlandt Park",
        "type": "park",
        "location": firestore.GeoPoint(40.8976, -73.8866),
        "description": "Sprawling park in the northwest Bronx with trails and a lake."
    },
    {
        "name": "Staten Island Greenbelt",
        "type": "park",
        "location": firestore.GeoPoint(40.5870, -74.1180),
        "description": "Vast network of woodland parks and trails in central Staten Island.",
        "wiki": "Staten Island Greenbelt"
    },
    {
        "name": "Clove Lakes Park",
        "type": "park",
        "location": firestore.GeoPoint(40.6175, -74.1057),
        "description": "Staten Island park with spring-fed lakes and old-growth trees."
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
    },
    # --- Outer-borough landmarks ---
    {
        "name": "Coney Island",
        "type": "landmark",
        "location": firestore.GeoPoint(40.5749, -73.9783),
        "description": "Iconic Brooklyn boardwalk, beach, and amusement area."
    },
    {
        "name": "Brooklyn Museum",
        "type": "landmark",
        "location": firestore.GeoPoint(40.6712, -73.9636),
        "description": "Major art museum beside the Brooklyn Botanic Garden."
    },
    {
        "name": "Barclays Center",
        "type": "landmark",
        "location": firestore.GeoPoint(40.6826, -73.9754),
        "description": "Arena in Downtown Brooklyn, home of the Brooklyn Nets."
    },
    {
        "name": "Grand Army Plaza",
        "type": "landmark",
        "location": firestore.GeoPoint(40.6743, -73.9702),
        "description": "Brooklyn plaza with the Soldiers' and Sailors' Memorial Arch at Prospect Park.",
        "wiki": "Grand Army Plaza"
    },
    {
        "name": "Unisphere",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7466, -73.8447),
        "description": "Giant steel globe from the 1964 World's Fair in Flushing Meadows, Queens."
    },
    {
        "name": "Citi Field",
        "type": "landmark",
        "location": firestore.GeoPoint(40.7571, -73.8458),
        "description": "Ballpark in Flushing, Queens, home of the New York Mets."
    },
    {
        "name": "Yankee Stadium",
        "type": "landmark",
        "location": firestore.GeoPoint(40.8296, -73.9262),
        "description": "Home of the New York Yankees in the South Bronx."
    },
    {
        "name": "Bronx Zoo",
        "type": "landmark",
        "location": firestore.GeoPoint(40.8506, -73.8769),
        "description": "One of the largest metropolitan zoos in the United States."
    },
    {
        "name": "New York Botanical Garden",
        "type": "landmark",
        "location": firestore.GeoPoint(40.8623, -73.8770),
        "description": "Historic botanical garden and landmark in the Bronx."
    },
    {
        "name": "Staten Island Ferry",
        "type": "landmark",
        "location": firestore.GeoPoint(40.6437, -74.0735),
        "description": "Free ferry to Manhattan; St. George Terminal on Staten Island.",
        "wiki": "Staten Island Ferry"
    },
    {
        "name": "Snug Harbor Cultural Center",
        "type": "landmark",
        "location": firestore.GeoPoint(40.6437, -74.1024),
        "description": "Historic cultural center and botanical gardens on Staten Island.",
        "wiki": "Snug Harbor Cultural Center"
    }

]

# Step 3: Resolve a curated photo for each POI, then upsert into Firestore.
# Idempotent: each POI is written to a deterministic doc ID (its name slug) with
# set(merge=True), so re-running updates the same docs in place instead of
# deleting + re-adding. No duplicates, no empty window, and — crucially — each
# POI's `comments` subcollection stays attached because its parent ID never
# changes. (A POI removed from this list is NOT auto-deleted; remove it by hand
# if needed.)
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

    doc_ref = db.collection("pois").document(slugify(poi["name"]))
    # Stamp createdAt only when the POI is new, so re-seeds preserve the original
    # creation time (overwriting it every run would not be idempotent).
    if not doc_ref.get().exists:
        poi["createdAt"] = datetime.utcnow()
    doc_ref.set(poi, merge=True)

print(f"✅ Upserted {len(pois)} POIs to Firestore "
      f"({with_photo} with photos, {len(pois) - with_photo} fallback).")
