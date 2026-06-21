# Echo — QA Remediation Plan

**Date:** 2026-06-18
**Source:** Derived from [QA_AUDIT.md](QA_AUDIT.md), after independently verifying each finding against the actual code, Google Play policy (via Google Developer Knowledge), and [architecture.md](architecture.md).
**Status:** Batches A1, A2, B, C complete. A3, A4, and Batch D remain.

## Verification summary

The audit's **code-level findings are accurate and well-reasoned.** The one finding that did not survive scrutiny is its headline severity-#1 item:

- **Admin key "unprotected in OneDrive" — materially wrong / overstated.** The key is **not** in OneDrive (the whole tree was searched: no `Echo` folder there, zero service-account JSONs). It lives at `scripts/firebase-key.json`, inside the repo but **gitignored** (`.gitignore:29`). It is a genuine live `echo-2b5ba` admin key, so the only residual is ordinary local hygiene — not "anyone with your OneDrive owns your backend." Downgraded from #1 severity to optional. The audit predates the recent *"relocate seeding scripts"* commit.

Confirmed valid:
- **App ID `com.example.echo`** — `app/build.gradle.kts:28` & `:37`. Play Console rejects `com.example` uploads. Permanent once published.
- **Guests can read every user's email** — `firestore.rules:151` is `allow read: if isSignedIn()`; `email` is stored in the profile doc.
- **Google users can't delete account in-app** — `SettingsScreen.kt:171-178` shows "isn't available yet"; backend `deleteAccountWithGoogle` / `ReauthCredential.Google` already exist. Google Play policy confirmed: in-app deletion required regardless of sign-in method.
- All spot-checked medium items (commentCount rule, missing feed/alerts list keys, non-transactional `toggleLike`, full-collection `getPostsByTag`, unshared `observeHiddenUserIds`, test-nav libs shipped, nav RC version, dead code, `allowBackup` with stub rule files).
- Architecture doc drift: `data/model`+`Dto` (doc) vs `data/entity`+`Entity` (code).

---

## Batch A — Launch blockers

### A1. Rename applicationId — ✅ DONE
- `namespace` + `applicationId` → `dev.echoapp.echo` (`app/build.gradle.kts`).
- All 149 Kotlin source files renamed; source directories moved from `com/example/echo/` → `dev/echoapp/echo/` across main, test, androidTest, debug, and release source sets.
- `proguard-rules.pro` keep rule updated.
- New Firebase Android app registered (`dev.echoapp.echo`); debug SHA-1 (`B7:E9:…:E4`) + release SHA-1 (`86:B0:…:4D`) added; fresh `google-services.json` downloaded and in place.
- Build verified green (`assembleDebug` + `testDebugUnitTest`).

### A2. Remove email from the publicly-readable profile — ✅ DONE
**Decision:** dropped the `email` field entirely instead of moving it to a private subdoc. Verified that nothing in the app ever reads `UserProfile.email`; every place that needs the user's own email reads `FirebaseAuth.currentUser.email` (e.g. `ProfileScreen.kt:45`, `PoiDetailViewModel.kt:53`). If no one reads it, there's no reason to store it — this fully removes the exposure with less surface area than a subdoc.
- **Rules:** removed the `request.resource.data.email == request.auth.token.email` check and dropped `email` from the create `hasOnly` set (`firestore.rules`).
- **Data layer:** removed `email` from `UserProfileEntity`, the `UserProfile` domain model, the profile write map, and both `UserProfile` constructions in `UserRepositoryImpl`.
- **Migration:** `scripts/strip_profile_emails.py` deletes the `email` field from existing `users/*` docs (idempotent). **Must be run once against the live project** to clean already-stored emails.
- **Not done (optional):** tightening profile reads from `isSignedIn()` to `isNotAnonymous()` — left as-is; revisit if guests don't need to read profiles.

### A3. Wire Google-account deletion *(blocked: Google Sign-In not configured)* ⏳
- `SettingsScreen.kt:171-178`: replace the "isn't available yet" snackbar in the `else` branch with a Credential Manager re-auth flow that obtains a Google ID token, then calls `viewModel.deleteAccountWithGoogle(idToken)` (`SettingsViewModel.kt:90`).
- **Dependency:** real `default_web_client_id` / configured Google Sign-In. Can't be tested end-to-end until that's done. Use the Credential Manager / `verified-email` skill as reference.

### A4. Rotate the admin key *(downgraded — optional hygiene)* ⏳
Not urgent: `scripts/firebase-key.json` is gitignored and not cloud-synced. If desired: regenerate in Console, replace the local file (same path, no code change). Skip unless exposure is suspected.

---

## Batch B — Quick wins ✅ DONE
1. ✅ Feed list key: add `key = { it.id }` at `FeedScreen.kt:227`.
2. ✅ Alerts list key: add `key = { it.postId }` at `AlertsScreen.kt:84`.
3. ✅ commentCount floor: require result `>= 0` in `firestore.rules:67-71` (interim; real fix is a Cloud Function).
4. ✅ Comment-delete gate: `firestore.rules:118` & `:138` → `isNotAnonymous()` to match create.
5. ✅ Nav test libs: moved to `androidTestImplementation`; dropped non-existent `-android` suffixed artifacts entirely.
6. ✅ Nav version alignment: all nav refs pinned to stable `2.8.9`; RC dropped.
7. ✅ Backup rules: set `allowBackup="false"` (`AndroidManifest.xml:11`).
8. ✅ Dead code: deleted `getPostsWithLocation`, `getPostsByUsername`, `refreshPosts` + `PoiMapper.toEntity`, `GetPostByIdUseCase`.
9. ✅ Architecture doc: updated `architecture.md` `data/model`+`Dto` → `data/entity`+`Entity`.
10. ✅ Committed `local.defaults.properties` with the placeholder so clean builds don't get a blank map.

---

## Batch C — Robustness ✅ DONE
- ✅ `toggleLike` → wrapped in `runTransaction` (`PostRepositoryImpl.kt`). (Can't use `arrayUnion`/`increment` — rules check concrete array == likeCount.)
- ✅ `observeHiddenUserIds` shared via `shareIn(appScope, WhileSubscribed(5000), replay=1)` (`UserRepositoryImpl.kt`).
- ✅ `getPostsByTag` → server-side `whereArrayContains` + `limit(200)` one-shot fetch; tags normalized (trim/lowercase/distinct) on write in `PostMapper`.
- ✅ Map: viewport-overlap guard in `MapViewModel.updateVisibleBounds` — skips re-fetch when new bounds are fully inside the current padded box.
- ✅ All 4 comment add/delete paths use `WriteBatch` so comment doc + counter are atomic (`CommentRepositoryImpl`).
- ✅ All 13 UI screens migrated from `collectAsState()` → `collectAsStateWithLifecycle()`; `lifecycle-runtime-compose` dependency added.

---

## Batch D — Consistency & tests 🔶 IN PROGRESS

**Done (layering + auth hardening):**
- ✅ Layering leak — `DeleteAccountUseCase` no longer imports Firebase types; the Firebase-exception → friendly-message mapping moved into `UserRepositoryImpl.deleteAccount` (data layer). Use case is now a clean pass-through.
- ✅ Layering leak — `FeedViewModel`, `PostDetailViewModel`, `PoiDetailViewModel` no longer inject `FirebaseAuth`; they read the current user via `AuthRepository.getCurrentUser()` (returns the `User` domain model with `id`/`email`/`isAnonymous`). Also removed dead Firebase imports from `ForgotPasswordScreen`.
- ✅ Preserve exception causes — `AuthRepositoryImpl` sign-in/up/guest/Google now wrap with `Exception(msg, e)` so the original cause survives.
- ✅ Password-reset generic message — `sendPasswordResetEmail` swallows `FirebaseAuthInvalidUserException` and reports success regardless, so the screen can't be used to tell which emails have accounts (enumeration).
- ✅ Sign-up enumeration — removed the proactive `fetchSignInMethods` "account already exists" probe from `SignUpScreen` (debounced disclosure of registered emails). Duplicate emails are still caught at submit via `signUpWithEmail`. Removed the now-dead `fetchSignInMethods` chain (VM method, `FetchSignInMethodsUseCase`, interface + impl).

**Remaining:**
- ✅ Error-handling convention (`Result` at the use-case boundary). Audit found it was already 5/7 consistent — repos throw, and the use cases adapt to `Result` (a valid translation boundary). Normalized the two outliers (`DeleteCommentUseCase`, `DeletePoiCommentUseCase`) to return `Result<Unit>` via `runCatching`; converted the two VM `try/catch` blocks to `.onFailure`. Deliberately did **not** push `Result` into the repo interfaces — that would just relocate the `try/catch` a layer down with no behavioral payoff.
- ✅ Seeding-script idempotency. `seed_pois.py` now writes each POI to a deterministic slug doc ID (e.g. `central-park`) with `set(merge=True)` instead of delete-all + `add()` (random IDs). Re-running updates the same docs in place — no duplicates, no empty window, POI `comments` subcollections stay attached. `createdAt` is stamped only on first creation. One-time wipe of the old random-ID docs done; collection re-seeded to 38 slug-ID POIs.
- ⏳ Test coverage in the audit's Section-4 order: pure helpers → repo guards → `FeedViewModel` → extracted clustering.

---

## Sequencing recommendation
A2 (email privacy) is the only blocker with no external dependency — start there. A1 and A3 need decisions/config (identifier; Google Sign-In setup), and A1 should precede A3 to avoid duplicate Sign-In work. Batch B can land in parallel anytime.
