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
*Last Updated: 2026-01-25*
