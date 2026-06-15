package com.example.echo.utils

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

    // Firestore Collections
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_POIS = "pois"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_USERNAMES = "usernames"


    // Firestore Fields
    const val FIELD_USERNAME = "username"
    const val FIELD_MESSAGE = "message"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_LIKES = "likes"
    const val FIELD_TAGS = "tags"

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
