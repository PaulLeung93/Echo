# 🗺️ Echo Roadmap — Path to Play Store

**Goal:** Get Echo to a production-ready, publishable state on the Google Play Store.
**Created:** 2026-06-10
**Companion docs:** [README.md](README.md) · [architecture.md](architecture.md) (source of truth)

This roadmap is phased: stabilize first, finish what's in-flight, clean the
structure, then harden for release. Phases are ordered by dependency — each one
assumes the previous is green.

---

## 🚦 Phase 0 — Stabilize (blocker) — ✅ COMPLETE (2026-06-10)

Resolved: the build was never actually broken — `build_errors.txt` was a stale
log; the `CommentCard.kt` `Column` import had already been fixed. Verified green
via a full `assembleDebug`, then smoke-tested on a Pixel_9_Pro emulator.

- [x] **Get to a green build.** `compileDebugKotlin` *and* a full `assembleDebug`
      both pass.
- [x] **Smoke-test the app** on emulator — launches without crash; sign-in,
      navigation, and Feed (live data) all render.
- [x] Delete `build_errors.txt` — done (stale log removed).

> **Side finding (resolved):** "Continue as Guest" failed with
> `ERROR_ADMIN_RESTRICTED_OPERATION` — the **Anonymous** sign-in provider was
> disabled in the Firebase console (not a code bug). Enabled it; guest sign-in
> now works end-to-end. Note for Phase 3: `mapFirebaseErrorMessage`
> ([FirebaseUtils.kt](app/src/main/java/com/example/echo/utils/FirebaseUtils.kt))
> swallows the real error into a generic message, which made this hard to
> diagnose — worth improving alongside auth edge cases + loading/error states.

---

## 🧩 Phase 1 — Finish the in-flight feature: POI Comments

You were mid-build on **proximity-based commenting on POIs**, mirroring the
post-comment feature. The screen + ViewModel exist and are wired into
`NavGraph` (`POI_DETAILS/{poiId}`), but the feature is unfinished.

- [ ] **Verify the end-to-end flow**: tap a POI marker → `PoiDetailScreen` →
      load comments → add comment → delete own comment.
- [ ] **Enforce the 5km proximity rule** for POI comments (the README promises
      it for posts; POIs should match). Currently the gate isn't applied here.
- [ ] **Fix comment-ownership check.** `PoiDetailScreen` decides delete-ability
      with `comment.username == uiState.currentUserEmail` — comparing a
      username to an email. Confirm and correct (compare on stable user id).
- [ ] **Fix the rate-limiter.** When the limit is exceeded the button silently
      does nothing (`// In a real app we'd show a snackbar`). Either surface a
      snackbar or remove the client-side limiter in favor of a real one
      (see Phase 3 — server-side rules).
- [ ] **Clean up `PoiDetailScreen`.** It contains a large block of rambling
      "thinking out loud" comments in the `LaunchedEffect`. Remove them.
- [ ] **Extract hardcoded strings** ("POI Details", "Comments", "Send", "Write a
      comment.", "Back", "Delete Comment") into `strings.xml` — required by
      architecture constraint #3 and for localization.

---

## 🧹 Phase 2 — Structural cleanup (dead code & drift)

Decision: **keep the documented `ui/` + `domain/usecase/` layout** (architecture.md
is the source of truth). Remove the competing half-migration and dead code.
*(Agent scaffolding — `GEMINI.md`, `.agent/` — is intentionally kept.)*

- [ ] **Collapse the `feature/` experiment.** Move
      `feature/map/presentation/{PoiDetailScreen,PoiDetailViewModel,PoiDetailUiState}.kt`
      into the documented tree (e.g. `ui/maps/` or a new `ui/poi/`), update
      package + imports + the `NavGraph` reference, then delete `feature/`.
- [ ] **Delete orphaned duplicate use cases:**
      `feature/map/domain/AddPoiCommentUseCase.kt` and
      `feature/map/domain/GetPoiCommentsUseCase.kt` (the live VM uses the
      `domain/usecase/comment/...` versions). Keep the `domain/usecase/comment/`
      copies.
- [ ] **Delete orphaned `ui/map/PoiDetailUiState.kt`** (singular `map`, nothing
      references it; superseded by the `feature/map/presentation` copy you'll
      relocate).
- [ ] **Delete the legacy `models/` package** (`Comment.kt`,
      `PointsOfInterest.kt`, `Post.kt`) — pre-Clean-Architecture leftovers,
      only referenced by themselves; superseded by `domain/model/`.
- [ ] **Reconcile the agent docs.** `GEMINI.md` references `ARCHITECTURE.md`
      (uppercase) and `DEVLOG.md` that don't exist. Either create a `DEVLOG.md`
      or update `GEMINI.md` to point at the real `architecture.md`.
- [ ] **Re-verify build is green** after the moves.

---

## 🔒 Phase 3 — Production hardening (the real Play Store work)

These are the items that separate a demo from a publishable app.

### Security & data integrity
- [ ] **Firestore Security Rules.** Author and deploy rules so users can only
      write their own posts/comments, can't spoof authorship, and POIs are
      read-only to clients. *Do not ship with open/test rules.*
- [ ] **Server-side proximity & rate limiting.** Client-side checks are
      bypassable. Enforce the 5km comment rule and abuse/rate limits in
      Firestore rules or a Cloud Function.
- [ ] **Storage rules** for image uploads (size/type/auth limits).

### Robustness
- [ ] **Auth edge cases**: account-exists-with-different-credential, expired
      sessions, Google sign-in cancel, network loss, password-reset failures.
- [ ] **Consistent loading / empty / error states** across Feed, Map, Profile,
      PostDetail, PoiDetail (some screens have them, others don't).
- [ ] **Location permission UX**: runtime request, rationale, and a graceful
      degraded mode when the user denies location.
- [ ] **Crash & analytics**: wire up Firebase Crashlytics.

### Build & release
- [ ] **R8/ProGuard**: enable minify + shrink for release, add keep rules,
      verify the release build runs. *(The `r8-analyzer` skill can audit rules.)*
- [ ] **Release signing** config + a signed AAB build.
- [ ] **Target latest required API** and verify **edge-to-edge** (enforced on
      Android 15+). *(The `edge-to-edge` skill can assist.)*
- [ ] **Remove debug logging** added in `PoiRepository` (commit cd5f797) before
      release.

### Quality bar (architecture says "testable" — currently no tests)
- [ ] **Establish a test harness** (unit tests for use cases/mappers, ViewModel
      tests, a couple of UI/integration tests). *(The `testing-setup` skill can
      scaffold this.)*

---

## 🛍️ Phase 4 — Store listing & compliance

- [ ] **Privacy Policy** (required — the app uses location + auth + user data).
- [ ] **Play Data Safety form** declaring location, email, and content data.
- [ ] **Store assets**: app icon, feature graphic, phone/tablet screenshots,
      short + full description.
- [ ] **Refresh README media** — the `docs/gifs/*` referenced in README should
      reflect the final UI (including POI comments).
- [ ] **App content rating** questionnaire + target audience declaration.

---

## 💡 Phase 5 — Post-launch / nice-to-have features

Deferred until shipped; captured so they aren't lost.

- [ ] Push notifications (replies, nearby activity) via FCM.
- [ ] Post editing/deletion polish + report/block (moderation — may become
      required depending on Play policy for UGC apps).
- [ ] Richer map: clustering tuning, search, "recenter on me".
- [ ] Profile enhancements (avatar upload, bio, post history).
- [ ] Offline cache / Room layer for feed resilience.

---

### Suggested order of attack
**Phase 0 → 1 → 2** get you back to a clean, compiling, feature-complete state
(this is the bulk of the "finishing where you left off" work). **Phase 3** is
the largest and most important block for actually publishing. **4–5** follow.
