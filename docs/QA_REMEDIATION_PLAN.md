# Echo — QA Remediation Plan

**Date:** 2026-06-18
**Source:** Derived from [QA_AUDIT.md](QA_AUDIT.md), after independently verifying each finding against the actual code, Google Play policy (via Google Developer Knowledge), and [architecture.md](architecture.md).
**Status:** Plan only — no code changed yet.

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

### A1. Rename applicationId *(blocked: needs a real owned identifier — deferred)*
Permanent once published, so it must be right before first upload.
- `app/build.gradle.kts:28` `namespace`, `:37` `applicationId`.
- Regenerate `google-services.json` from a re-registered app in the Firebase Console (package name is baked in).
- Re-add SHA-1/SHA-256 fingerprints (debug + release/Play app-signing) to the new Firebase app — also affects Google Sign-In and the Maps key's package restriction.
- Move the source package `com.example.echo` → new package (large mechanical refactor; its own commit).
- **Dependency:** decide the identifier first. Do this *before* A3 to avoid re-doing Google Sign-In config twice.

### A2. Remove email from the publicly-readable profile — ✅ DONE
**Decision:** dropped the `email` field entirely instead of moving it to a private subdoc. Verified that nothing in the app ever reads `UserProfile.email`; every place that needs the user's own email reads `FirebaseAuth.currentUser.email` (e.g. `ProfileScreen.kt:45`, `PoiDetailViewModel.kt:53`). If no one reads it, there's no reason to store it — this fully removes the exposure with less surface area than a subdoc.
- **Rules:** removed the `request.resource.data.email == request.auth.token.email` check and dropped `email` from the create `hasOnly` set (`firestore.rules`).
- **Data layer:** removed `email` from `UserProfileEntity`, the `UserProfile` domain model, the profile write map, and both `UserProfile` constructions in `UserRepositoryImpl`.
- **Migration:** `scripts/strip_profile_emails.py` deletes the `email` field from existing `users/*` docs (idempotent). **Must be run once against the live project** to clean already-stored emails.
- **Not done (optional):** tightening profile reads from `isSignedIn()` to `isNotAnonymous()` — left as-is; revisit if guests don't need to read profiles.

### A3. Wire Google-account deletion *(blocked: Google Sign-In not configured)*
- `SettingsScreen.kt:171-178`: replace the "isn't available yet" snackbar in the `else` branch with a Credential Manager re-auth flow that obtains a Google ID token, then calls `viewModel.deleteAccountWithGoogle(idToken)` (`SettingsViewModel.kt:90`).
- **Dependency:** real `default_web_client_id` / configured Google Sign-In. Can't be tested end-to-end until that's done. Use the Credential Manager / `verified-email` skill as reference.

### A4. Rotate the admin key *(downgraded — optional hygiene)*
Not urgent: `scripts/firebase-key.json` is gitignored and not cloud-synced. If desired: regenerate in Console, replace the local file (same path, no code change). Skip unless exposure is suspected.

---

## Batch B — Quick wins (no external setup; one PR)
1. Feed list key: add `key = { it.id }` at `FeedScreen.kt:227`.
2. Alerts list key: add `key = { it.postId }` at `AlertsScreen.kt:84`.
3. commentCount floor: require result `>= 0` in `firestore.rules:67-71` (interim; real fix is a Cloud Function).
4. Comment-delete gate: `firestore.rules:118` & `:138` → `isNotAnonymous()` to match create.
5. Nav test libs: move `build.gradle.kts:110-111` to `androidTestImplementation`.
6. Nav version alignment: set all nav refs in `libs.versions.toml:17-20` to one stable version (drop `2.9.0-rc01`).
7. Backup rules: fill `data_extraction_rules.xml` / `backup_rules.xml` to exclude auth/datastore, or set `allowBackup="false"` (`AndroidManifest.xml:11`).
8. Dead code: delete `getPostsWithLocation`, `getPostsByUsername`, `refreshPosts` + use-case wrappers (`PoiMapper.toEntity`, `GetPostByIdUseCase`) — verify zero callers first.
9. Architecture doc: update `architecture.md` `data/model`+`Dto` → `data/entity`+`Entity`.
10. Commit `local.defaults.properties` with the placeholder so clean builds don't get a blank map.

---

## Batch C — Robustness (each independently testable)
- `toggleLike` → wrap read+write in `runTransaction` (`PostRepositoryImpl.kt:301`); fix the misleading "single write" comment. (Can't use `arrayUnion`/`increment` — rules check concrete array == likeCount.)
- Share `observeHiddenUserIds` via `shareIn` on app scope (`UserRepositoryImpl.kt:206`).
- `getPostsByTag` → server-side `whereArrayContains` + `limit`, one-shot fetch, normalize tag case on write (`PostRepositoryImpl.kt:125`).
- Map: add debounce + viewport-overlap check in `MapViewModel`.
- Batch comment write + counter (`CommentRepositoryImpl`).
- Migrate screens `collectAsState()` → `collectAsStateWithLifecycle()`.

---

## Batch D — Consistency & tests (later)
Error-handling convention (`Result` everywhere); preserve exception causes in `AuthRepositoryImpl`; password-reset generic message; layering leaks (`DeleteAccountUseCase` Firebase import; VMs injecting `FirebaseAuth`); seeding-script idempotency. Then test coverage in the audit's Section-4 order: pure helpers → repo guards → `FeedViewModel` → extracted clustering.

---

## Sequencing recommendation
A2 (email privacy) is the only blocker with no external dependency — start there. A1 and A3 need decisions/config (identifier; Google Sign-In setup), and A1 should precede A3 to avoid duplicate Sign-In work. Batch B can land in parallel anytime.
