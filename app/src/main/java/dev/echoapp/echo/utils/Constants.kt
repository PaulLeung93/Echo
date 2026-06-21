package dev.echoapp.echo.utils

object Constants {
    // Proximity
    /** Max distance (meters) a user may be from a POI/post to comment on it. */
    const val PROXIMITY_RADIUS_METERS = 5_000.0

    /**
     * User-Agent for hotlinking POI photos from Wikimedia. Their policy blocks
     * generic library UAs (e.g. "okhttp/…" → 403); a descriptive one with
     * contact info is required. See seed_pois.py for the matching seed-time UA.
     */
    const val IMAGE_USER_AGENT = "EchoApp/1.0 (Android; contact paul.leung@codepath.org)"

    /**
     * Max posts fetched by the live feed/map snapshot listener. The listener is
     * unbounded by nature (a `.limit()` newest-first cap), so without this every
     * session bills a read per post in the collection and grows forever. Caps the
     * Spark-plan read budget; the proper fix is geohash-based geo queries.
     */
    const val POSTS_QUERY_LIMIT = 200L

    /** Page size for the paginated feed (one billed read per post, per page loaded). */
    const val FEED_PAGE_SIZE = 25L

    /**
     * How long the cached POI list is trusted before a server re-sync. POIs are
     * admin-curated reference data, so a long TTL means a device bills POI reads at
     * most ~twice a day instead of once per session. (12 hours.)
     */
    const val POIS_CACHE_TTL_MS = 12 * 60 * 60 * 1000L

    /**
     * Minimum map zoom at which we load/show user posts. Posts are fetched by a
     * viewport-radius geo query with no count cap, so zooming out far enough would
     * read every located post in range (e.g. a whole country) — wasteful billed
     * reads and meaningless at that scale. Below this zoom we skip the query and
     * draw no post markers. ~12 ≈ city-district scale, matching the app's local feel.
     */
    const val MIN_POSTS_ZOOM = 12f

    /**
     * Minimum map zoom at which we draw POI markers. POIs are loaded from the local
     * cache (0 billed reads) so this is purely a declutter threshold — zoomed out far
     * enough, dozens of discs overlap into a blob. Set just below [MIN_POSTS_ZOOM] so
     * landmarks appear slightly before posts as you zoom in (a tiered reveal).
     */
    const val MIN_POIS_ZOOM = 11f

    /**
     * Default set of map marker-type filters (everything shown). Used both as the
     * initial state and as the fallback when no persisted preference exists yet.
     */
    val DEFAULT_MAP_FILTERS = setOf("user posts", "landmark", "park", "college")

    // Firestore Collections
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_POIS = "pois"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_USERNAMES = "usernames"
    const val COLLECTION_REPORTS = "reports"


    // Firestore Fields
    const val FIELD_USERNAME = "username"
    const val FIELD_MESSAGE = "message"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_LIKES = "likes"
    const val FIELD_TAGS = "tags"
    const val FIELD_GEOHASH = "geohash"

    // Navigation Routes
    const val ROUTE_FEED = "feed"
    const val ROUTE_SIGN_IN = "sign_in"
    const val ROUTE_SIGN_UP = "sign_up"
    const val ROUTE_CREATE_POST = "create_post"
    const val ROUTE_FORGOT_PASSWORD = "forgot_password"
    const val ROUTE_POST_DETAILS = "post_details"
    const val ROUTE_MAP = "map"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_POI_DETAILS = "poi_details"
    const val ROUTE_ALERTS = "alerts"
    const val ROUTE_COMPLETE_PROFILE = "complete_profile"
    const val ROUTE_EDIT_PROFILE = "edit_profile"
    const val ROUTE_SETTINGS = "settings"
}
