# Echo App Architecture

> **Version**: Pre-Refactoring Analysis  
> **Last Updated**: 2026-01-25  
> **Status**: Planning

---

## Architecture Overview

Echo is a location-based social networking app built with Kotlin and Jetpack Compose. This document tracks the architectural evolution as we refactor toward Clean Architecture with MAD best practices.

### Current State (Pre-Refactoring)

The app currently follows a flat structure without proper layer separation:
- **No DI**: ViewModels directly instantiate Firebase dependencies
- **No Repository Layer**: Database access mixed with UI logic
- **No Domain Layer**: Business logic embedded in ViewModels
- **Multiple StateFlows**: Violates Unidirectional Data Flow pattern
- **String-based Navigation**: Not type-safe

### Target Architecture (Post-Refactoring)

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Composable │  │  ViewModel  │  │   UiState   │     │
│  │   Screens   │──│  (Hilt DI)  │──│  + Events   │     │
│  └─────────────┘  └──────┬──────┘  └─────────────┘     │
└──────────────────────────┼──────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────┐
│                     Domain Layer                         │
│  ┌─────────────┐  ┌──────┴──────┐  ┌─────────────┐     │
│  │   Models    │  │  Use Cases  │  │ Repository  │     │
│  │   (Pure)    │  │  (Business) │  │ Interfaces  │     │
│  └─────────────┘  └─────────────┘  └──────┬──────┘     │
└──────────────────────────────────────────│──────────────┘
                                           │
┌──────────────────────────────────────────┼──────────────┐
│                      Data Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────┴───────┐     │
│  │  Entities   │  │   Mappers   │  │ Repository  │     │
│  │   (DTOs)    │──│ Entity↔Dom  │──│   Impls     │     │
│  └─────────────┘  └─────────────┘  └──────┬──────┘     │
└──────────────────────────────────────────│──────────────┘
                                           │
                              ┌────────────┴────────────┐
                              │   Firebase Firestore    │
                              │   Firebase Auth         │
                              └─────────────────────────┘
```

---

## File Tree (After Phase 1)

```
Echo/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/example/echo/
│       │   │   ├── MainActivity.kt
│       │   │   ├── components/
│       │   │   │   └── PostCard.kt
│       │   │   ├── domain/                          # NEW - Domain Layer
│       │   │   │   ├── model/
│       │   │   │   │   ├── Comment.kt               # Domain comment model
│       │   │   │   │   ├── Poi.kt                   # Domain POI model
│       │   │   │   │   ├── Post.kt                  # Domain post model
│       │   │   │   │   └── User.kt                  # Domain user model
│       │   │   │   ├── repository/
│       │   │   │   │   ├── AuthRepository.kt        # Auth operations interface
│       │   │   │   │   ├── CommentRepository.kt     # Comment operations interface
│       │   │   │   │   ├── PoiRepository.kt         # POI operations interface
│       │   │   │   │   └── PostRepository.kt        # Post operations interface
│       │   │   │   └── usecase/
│       │   │   │       ├── auth/
│       │   │   │       │   ├── GetCurrentUserUseCase.kt
│       │   │   │       │   ├── SignInWithEmailUseCase.kt
│       │   │   │       │   ├── SignInWithGoogleUseCase.kt
│       │   │   │       │   ├── SignOutUseCase.kt
│       │   │   │       │   └── SignUpWithEmailUseCase.kt
│       │   │   │       ├── comment/
│       │   │   │       │   ├── AddCommentUseCase.kt
│       │   │   │       │   └── GetCommentsUseCase.kt
│       │   │   │       ├── poi/
│       │   │   │       │   └── GetPoisUseCase.kt
│       │   │   │       └── post/
│       │   │   │           ├── CreatePostUseCase.kt
│       │   │   │           ├── DeletePostUseCase.kt
│       │   │   │           ├── GetPostByIdUseCase.kt
│       │   │   │           ├── GetPostsUseCase.kt
│       │   │   │           ├── ToggleLikeUseCase.kt
│       │   │   │           └── UpdatePostUseCase.kt
│       │   │   ├── models/                          # LEGACY - Will be migrated
│       │   │   │   ├── Comment.kt
│       │   │   │   ├── PointsOfInterest.kt
│       │   │   │   └── Post.kt
│       │   │   ├── navigation/
│       │   │   │   ├── NavGraph.kt
│       │   │   │   └── RootNavHost.kt
│       │   │   ├── ui/
│       │   │   │   ├── auth/
│       │   │   │   ├── common/
│       │   │   │   ├── create/
│       │   │   │   ├── feed/
│       │   │   │   ├── maps/
│       │   │   │   ├── post/
│       │   │   │   ├── profile/
│       │   │   │   ├── splash/
│       │   │   │   └── theme/
│       │   │   └── utils/
│       │   └── res/
│       ├── test/java/com/example/echo/
│       └── androidTest/java/com/example/echo/
├── gradle/
│   └── libs.versions.toml                           # UPDATED with Hilt, testing deps
├── build.gradle.kts
├── settings.gradle.kts
├── architecture.md                                  # NEW
└── README.md
```

---

## File Registry

| File | Responsibility |
|------|----------------|
| `EchoApplication.kt` | Hilt Application class |
| `AppModule.kt` | DI module for Firebase instances |
| `DispatcherModule.kt` | DI module for Coroutine dispatchers |
| `RepositoryModule.kt` | DI module binding repository interfaces |
| `Qualifiers.kt` | Dispatcher qualifier annotations |
| `PostEntity.kt` | Firestore entity for posts |
| `CommentEntity.kt` | Firestore entity for comments |
| `PoiEntity.kt` | Firestore entity for POIs |
| `PostMapper.kt` | Mapper for Post/PostEntity conversion |
| `CommentMapper.kt` | Mapper for Comment/CommentEntity conversion |
| `PoiMapper.kt` | Mapper for Poi/PoiEntity conversion |
| `UserMapper.kt` | Mapper for FirebaseUser/User conversion |
| `PostRepositoryImpl.kt` | Firestore implementation of PostRepository |
| `AuthRepositoryImpl.kt` | Firebase implementation of AuthRepository |
| `CommentRepositoryImpl.kt` | Firestore implementation of CommentRepository |
| `PoiRepositoryImpl.kt` | Firestore implementation of PoiRepository |
| `MainActivity.kt` | Main entry point hosting Compose NavHost |
| `PostCard.kt` | Reusable composable for displaying post content with likes/comments |
| `Comment.kt` | Data class representing a comment on a post |
| `PointsOfInterest.kt` | Data class for POI with Firebase GeoPoint |
| `Post.kt` | Data class representing a user post with location and metadata |
| `NavGraph.kt` | Composable navigation graph defining all app routes |
| `RootNavHost.kt` | Root composable setting up navigation controller |
| `AuthViewModel.kt` | ViewModel handling authentication state and Firebase Auth operations |
| `ForgotPasswordScreen.kt` | Composable screen for password reset flow |
| `SignInScreen.kt` | Composable screen for email/password and Google sign-in |
| `SignUpScreen.kt` | Composable screen for user registration |
| `BottomNavigationBar.kt` | Reusable bottom navigation component |
| `TopSnackBarHost.kt` | Snackbar host positioned at top of screen |
| `CreatePostScreen.kt` | Composable screen for creating new posts with location |
| `CreatePostViewModel.kt` | ViewModel handling post creation and validation |
| `FeedScreen.kt` | Composable screen displaying list of posts with filtering |
| `FeedUiState.kt` | Sealed class representing feed loading/success/error states |
| `FeedViewModel.kt` | ViewModel managing feed data and user interactions |
| `CreateClusterIcon.kt` | Utility for creating cluster marker icons on map |
| `MapScreen.kt` | Composable screen with Google Maps showing posts and POIs |
| `MapUiState.kt` | Sealed class representing map loading/success/error states |
| `MapUtils.kt` | Map-related utility functions |
| `MapViewModel.kt` | ViewModel managing map markers, clusters, and filtering |
| `MarkerTypeFilterDialog.kt` | Dialog for filtering map markers by type |
| `PostDetailScreen.kt` | Composable screen showing post with comments |
| `PostDetailViewModel.kt` | ViewModel managing post detail, likes, and comments |
| `ProfileScreen.kt` | Composable screen showing user's posts and stats |
| `ProfileViewModel.kt` | ViewModel managing profile data and post management |
| `SplashScreen.kt` | Initial splash screen during auth check |
| `Color.kt` | Theme color definitions |
| `Theme.kt` | Material 3 theme configuration |
| `Type.kt` | Typography definitions |
| `Constants.kt` | Firestore collection names and route constants |
| `DateUtils.kt` | Date formatting utilities |
| `FirebaseUtils.kt` | Firebase helper functions |
| `ValidationUtils.kt` | Input validation utilities |
| `strings.xml` | String resources (currently only app_name) |
| `ExampleUnitTest.kt` | Placeholder unit test |
| `ExampleInstrumentedTest.kt` | Placeholder instrumented test |
| `libs.versions.toml` | Gradle version catalog |

---

## Data Flow (Current)

```
User Action → Composable → ViewModel.method() → Firebase.getInstance() → Firestore
                              ↓
                         StateFlow.emit()
                              ↓
                         Composable.collectAsState()
```

## Data Flow (Target - UDF)

```
User Action → Composable → ViewModel.onAction(Intent)
                              ↓
                         Use Case → Repository → DataSource → Firestore
                              ↓
                         StateFlow<UiState>.emit()
                              ↓
                         Composable.collectAsStateWithLifecycle()
```

---

## Key Issues to Address

1. **ViewModels directly instantiate Firebase** - Violates DI principles, untestable
2. **Multiple StateFlows per ViewModel** - Should consolidate to single `UiState`
3. **No Repository abstraction** - Data layer mixed with presentation
4. **String-based navigation** - Not type-safe, error-prone
5. **Hardcoded strings in UI** - Only `app_name` in strings.xml
6. **No real unit tests** - Only placeholder tests exist
7. **Package mismatch** - `MapViewModel` declares `package com.example.echo.ui.map` but is in `maps/` folder
