# Echo App - Points of Interest (POI) Documentation

This document explains how the Points of Interest (POI) system is implemented in the Echo app, covering the data source, processing pipeline, and UI integration.

## 1. Data Source & Origin
The POI data originates from a Python seeding script located on the local machine and is stored in Firebase Firestore.

- **Primary Source File**: `C:\Users\Paul\OneDrive\Desktop\Echo\seed_pois.py`
- **Authentication**: Uses a service account key at `C:\Users\Paul\OneDrive\Desktop\Echo\firebase-key.json`.
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
1. Open the local script: `C:\Users\Paul\OneDrive\Desktop\Echo\seed_pois.py`.
2. Update the `pois` list in the Python dictionary.
3. Run the script: `python seed_pois.py`.
4. The changes will propagate to all active app instances via Firestore's real-time sync.

---

## 4. POI Comments (in-flight feature)

POIs support their own comment thread, separate from post comments, mirroring the
proximity-based commenting model used for posts.

### Data Structure (Firestore Schema)
- **Subcollection**: `pois/{poiId}/comments/{commentId}`, same shape as post comments
  (`username`, `message`, `timestamp`).
- **`commentCount` (Number)**: Stored on the parent `pois/{poiId}` document. Kept in sync
  via `FieldValue.increment(1)` / `increment(-1)` whenever a comment is added or deleted.
  Surfaced through `PoiEntity.commentCount` → `Poi.commentCount`.

### Technical Implementation
- **`CommentRepositoryImpl.kt`**: Extends the existing post-comment repository with
  `getCommentsForPoi`, `addCommentToPoi`, and `deleteCommentFromPoi`, using the
  `pois/{poiId}/comments` subcollection and incrementing/decrementing `commentCount` on
  the POI document.
- **Use Cases** (`domain/usecase/comment/`):
  - `GetPoiCommentsUseCase` — streams comments for a POI.
  - `AddPoiCommentUseCase` — validates the user is signed in (non-anonymous) and the
    message is non-blank before delegating to the repository.
  - `DeletePoiCommentUseCase` — deletes a comment by id.
- **Presentation**: `PoiDetailViewModel` + `PoiDetailScreen`
  (`feature/map/presentation/`) load the POI (via `getPoiByIdFlow`) and its comments,
  and expose add/delete actions. Wired into `NavGraph` at
  `${Destinations.POI_DETAILS}/{poiId}`, navigated to from map markers.

### Status / Known Gaps
Per `ROADMAP.md` Phase 1, this feature is functional but unfinished:
- The 5km proximity rule (enforced for post comments) is **not yet enforced** for POI
  comments.
- Comment-ownership check for delete compares `comment.username` to the current user's
  **email**, which needs correcting to a stable user id.
- `feature/map/presentation/` and `feature/map/domain/` currently duplicate some files
  under `domain/usecase/comment/` and `ui/map/` — the roadmap calls for collapsing this
  into the documented `ui/` tree.

---
*Last Updated: 2026-06-10*
