# Project Implementation Tracking

## 📋 Task Checklist

### Phase 0: Environment Setup
- [x] Configure `libs.versions.toml` (Version Catalog).
- [x] Setup `build.gradle.kts` with Hilt, Compose, and Firebase.
- [x] Initialize Project Structure (Folders & Packages).
- [x] Verify "Hello World" build and Firebase connectivity.

### Phase 1: Domain Layer
- [x] Define Domain Entities (`Post`, `User`, `Poi`).
- [x] Create Repository Interfaces.
- [x] Implement core Use Cases (Auth, Posts, POIs).

### Phase 2: Data & DI
- [x] Implement DTOs for Firebase.
- [x] Develop Mapper classes (DTO -> Entity).
- [x] Implement Repository logic for Auth, Firestore, and Storage.
- [x] Configure Hilt Modules for Dependency Injection.
- [x] Add Unit Tests for Mappers.

### Phase 3: Presentation & Navigation
- [x] Setup Type-safe Navigation Graph.
- [x] Implement Auth Screens (SignIn, SignUp).
- [x] Implement Feed Screen with UDF.
- [x] Implement Map Screen with POI clustering.
- [x] Implement Post Creation and Details screens.
- [x] Finalize `architecture.md` and `implementation.md`.

### Phase 4: Quality & Verification
- [ ] Comprehensive Unit Testing for ViewModels.
- [ ] Runtime Verification via MCP (Manual testing of all flows).
- [ ] Address any reported UI/Logic bugs.

---

## ⚠️ Key Issues & Technical Debt

| Priority | Issue | Description |
| :--- | :--- | :--- |
| **High** | Hardcoded Strings | Many UI components (e.g., `SignInScreen`, `FeedScreen`) still contain hardcoded string literals. Must move to `strings.xml`. |
| **Medium** | ViewModel Test Coverage | Unit tests for ViewModels (especially Auth and CreatePost) are missing or sparse. Need comprehensive Coverage. |
| **Medium** | Dispatcher Abstraction | Ensure all background tasks in ViewModels use injected `CoroutineDispatchers` rather than `Dispatchers.IO` directly. |
| **Low** | Image Compression | Implement client-side image compression before uploading to Firebase Storage to optimize bandwidth. |

---

## 🚀 Next Up
- **Immediate Focus**: Perform runtime verification of the current implementation to ensure all features work as expected before declaring the project ready.
- **Task**: Open the app and verify the Login -> Feed -> Map flow.
