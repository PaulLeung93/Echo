# Echo App - Points of Interest (POI) Documentation

This document explains how the Points of Interest (POI) system is implemented in the Echo app, covering the data source, processing pipeline, and UI integration.

## 1. Data Source & Origin
The POI data originates from a Python seeding script in the repo and is stored in Firebase Firestore.

- **Primary Source File**: `scripts/seed_pois.py`
- **Authentication**: Uses a service account key at `scripts/firebase-key.json` (gitignored — never committed; place it there manually before running).
- **Database**: Cloud Firestore (Project: `echo-2b5ba`)
- **Collection Name**: `pois`

### Data Structure (Firestore Schema)
Each document in the `pois` collection follows this format:
- `name` (String): Display name of the location.
- `type` (String): Category of the POI (`college`, `park`, or `landmark`).
- `location` (GeoPoint): Precise latitude and longitude.
- `description` (String): Short summary of the location.
- `createdAt` (Timestamp): Set by the seeding script during upload.

## 2. Technical Implementation

### Repository Layer (`PoiRepositoryImpl.kt`)
The app uses a real-time listener to fetch POIs from Firestore:
- **Snapshot Listener**: Uses `poisCollection.addSnapshotListener` to listen for real-time updates. If the database changes (via the seed script or Firebase Console), the app UI updates automatically.
- **Mapping**: Converts Firestore `GeoPoint` and document data into a domain-level `Poi` model.

### Domain Layer
- **`Poi.kt`**: A pure Kotlin data class representing the POI, decoupled from Firebase dependencies.
- **`GetPoisUseCase.kt`**: Provides a clean interface for the ViewModel to observe the stream of POIs.

### Presentation Layer (`MapViewModel.kt`)
The ViewModel manages how POIs are prepared for the map:
- **Filtering**: Combines the POI stream with `activeFilters` (stateful set of types).
- **State Management**: Exposes a `MapUiState` containing the filtered list of POIs to be rendered by the `MapScreen`.

## 3. How to Update POIs
To add or modify POIs:
1. Open the script: `scripts/seed_pois.py`.
2. Update the `pois` list in the Python dictionary.
3. Run from the `scripts/` folder (so it finds `firebase-key.json`): `cd scripts && python seed_pois.py`.
4. The changes will propagate to all active app instances via Firestore's real-time sync.

---

## 4. POI Post Threads

A POI is a **thread of posts**. Instead of a lightweight comment box, each POI gathers
full posts (likeable, comment-able, individually openable like any feed post). This
replaced the earlier POI-comments feature.

### Data Model
A POI post is an ordinary document in the top-level `posts` collection carrying two extra
denormalized fields:
- **`poiId` (String)** — the POI the post belongs to. Drives the thread query
  (`posts where poiId == {poiId} orderBy timestamp`). Absent on ordinary feed posts.
- **`poiName` (String)** — the POI's name, copied at create time so the feed badge can
  show the place name without an extra read. Validated against the real POI by the
  security rules (`isValidPoiRef`) so it can't be spoofed.

POI posts appear everywhere ordinary posts do (feed + map). The POI's location is snapped
onto the post at create time. A denormalized **`postCount`** on the `pois/{poiId}` document
tracks thread size (was `commentCount`); it is incremented in the create batch and
decremented by the cascade-delete Cloud Function.

### Technical Implementation
- **`PostRepositoryImpl`**: `getPostsForPoi(poiId, descending)` streams the thread;
  `createPost(..., poiId, poiName)` writes the post and bumps the POI `postCount` in one
  batch when `poiId` is set.
- **Use Case**: `GetPoiPostsUseCase` (`domain/usecase/post/`) wraps the thread stream.
- **Create flow**: `CreatePostScreen` is launched as `create_post?poiId={poiId}`;
  `CreatePostViewModel` loads the POI, snaps the location to it, and hides the location
  toggle (showing a "Posting to {name}" banner instead).
- **Presentation**: `PoiDetailViewModel` + `PoiDetailScreen` (`ui/poi/`) load the POI
  (via `getPoiByIdFlow`) and its post thread, with a newest/oldest sort toggle and an
  "Add post" button gated by the 5km proximity rule. Each post is a reused `PostCard`
  (like / report / block; tap opens `PostDetailScreen`). Wired into `NavGraph` at
  `${Destinations.POI_DETAILS}/{poiId}`, navigated to from map markers and from POI-post
  badges in the feed.
- **Cascade delete**: `functions/index.js` `onPostDeleted` deletes a deleted post's
  `comments` subcollection and decrements its POI's `postCount`.

### Proximity
Adding a post to a POI requires being within `Constants.PROXIMITY_RADIUS_METERS` (5km),
client-gated via `PoiDetailUiState.canPost` (same model as before). As with all posts,
`firestore.rules` cannot enforce proximity (no trusted location source); see the rules
header and ROADMAP Phase 3.

### Migration note
The previous `pois/{poiId}/comments` subcollections are no longer read or written. They
can be left in place (harmless) or removed with `scripts/drop_poi_comments.py`.

---
*Last Updated: 2026-06-21*
