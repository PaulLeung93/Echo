---
trigger: always_on
---

# Android Project Standards & Architecture Guidelines

## 1. Tech Stack (Strict)
* **Language:** Kotlin (Latest version).
* **UI:** Jetpack Compose (Material 3). **No XML layouts**.
* **Navigation:** Jetpack Navigation Compose (Type-Safe Routes).
* **Dependency Injection:** Hilt.
* **Asynchronous:** Coroutines & Flow. **No RxJava**.
* **Networking:** Retrofit + OkHttp (Include only if the app communicates with a remote API).
* **Image Loading:** Coil.
* **Local DB:** Room (Use only if local persistence/offline support is required).
* **Logging:** Timber (Do not use `android.util.Log`).
* **Build System:** Gradle (Kotlin DSL) with Version Catalogs (`libs.versions.toml`).

## 2. Architecture Principles
Follow **Clean Architecture** with MVVM (Model-View-ViewModel).

### Layers
1.  **UI Layer (Presentation):**
    * `Activity`: Single Activity Architecture (`MainActivity`).
    * `Composables`: Pure UI functions.
    * `ViewModel`: Holds `UiState` (StateFlow). Handles user `Events`.
2.  **Domain Layer:**
    * `UseCases`: Encapsulate single business actions. Pure Kotlin.
3.  **Data Layer:**
    * `Repository`: Single source of truth. Exposes data as `Flow<T>`.
    * `DataSource`: Remote (API) or Local (DAO).

### Data Flow (UDF)
* **Events (Up):** UI sends events to ViewModel (e.g., `onButtonClicked()`).
* **State (Down):** ViewModel exposes a single `uiState` (StateFlow).
* **Immutability:** `UiState` must be a Data Class with `val` properties.

## 3. Coding Standards

### Jetpack Compose
* **State Hoisting:** Never hold state inside a reusable composable. Pass `T` down and `(T) -> Unit` events up.
* **Previews:** Every UI component must have a `@Preview`.
* **Modifiers:** Always allow the caller to pass a `Modifier` as the first optional argument.
* **Lists:** Always provide a `key` lambda to `items()` in LazyColumn/Row for performance.
* **Lifecycle:** Always use `collectAsStateWithLifecycle()` (from `androidx.lifecycle:lifecycle-runtime-compose`) for safe flow collection in UI.

### Coroutines
* Use `viewModelScope` in ViewModels.
* Use `suspend` functions for one-shot operations.
* **Dispatchers:** Never hardcode `Dispatchers.IO`. Inject dispatchers to allow testing.

## 4. Testing & Quality
* **Unit Tests:** JUnit 5 + Mockk + Turbine (for Flows).
* **UI Tests:** Compose Test Rule.
* **Formatting:** Code must pass `ktlint` checks.
* **Device Verification (Strict):**
    * You MUST verify all UI changes on the connected device using the available **MCP (Model Context Protocol)**.
    * Do not assume code works just because it compiles.
    * Perform a "Visual Check" (screenshot or interaction) for every new feature.

## 5. Security & Secrets
* **API Keys:** NEVER hardcode API keys. Use `local.properties` / BuildConfig.
* **Exported:** Ensure Android components are not exported (`exported=false`) unless necessary.

## 6. File Organization (Package by Feature)
**Strictly follow this structure:**
* `com.example.app`
    * `core/` (Common UI, utilities, di)
    * `feature/`
        * `auth/`
            * `presentation/` (LoginScreen, LoginViewModel)
            * `domain/` (LoginUseCase)
            * `data/` (AuthRepositoryImpl)
        * `profile/`
            * `presentation/`
            * `domain/`
            * `data/`

## 7. Operational Protocols & Agent Instructions (Meta)
* **No Placeholders:** Never leave `// TODO: Implement logic` or `// ... rest of code`. You must write the full implementation.
* **Preserve Context:** Do not delete existing comments or documentation when modifying a file.
* **Path Verification:** Never guess a file path. Use `ls` or `find` if you are unsure where a file lives.
* **Self-Correction:** If a build fails, you MUST read the error log using `android-build` or `gradle-runner` before asking the user for help. Try to fix it at least once.
* **Dependencies:** Always check `libs.versions.toml` before adding libraries.
* **Imports:** Always optimize imports. Do not use wildcard imports (`import android.util.*`).

## 8. System Design & Performance (Strict)
* **Persistence Strategy (Condition):**
    * **If Offline Support is required:** The Repository must use **Room** as the Single Source of Truth. (API writes to DB -> UI reads from DB).
    * **If Online Only:** The Repository may fetch directly from API, but must return `Result<T>` or `Flow<Result<T>>` to handle flaky networks.
* **Background Work:** Use **WorkManager** for uploads/syncing (tasks > 30s). Do not use long-running Coroutines in ViewModels.
* **Image Optimization:** Coil requests must use `.crossfade(true)`.
* **Memory Leaks:**
    * Never pass `Context` to ViewModels.
    * Stop location/sensor updates in `onPause` (or `DisposableEffect` in Compose).

## 9. Dependency Strategy (Strict)
* **Bill of Materials (BOM):**
    * **Compose:** MUST use `androidx.compose:compose-bom`. Do not specify versions for `ui`, `material3`, or `ui-tooling`.
    * **OkHttp:** MUST use `com.squareup.okhttp3:okhttp-bom`.
    * **Firebase:** MUST use `com.google.firebase:firebase-bom`.
* **Stability:** Do not use `alpha`, `beta`, or `rc` versions unless the user explicitly requests a "cutting edge" feature. Always prefer `stable`.
* **Kotlin Versioning:** Ensure library versions are compatible with the project's current Kotlin Compiler Extension (check `build.gradle.kts` `composeOptions`).