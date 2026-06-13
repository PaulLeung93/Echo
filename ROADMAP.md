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

## 🧩 Phase 1 — Finish the in-flight feature: POI Comments — ✅ COMPLETE (2026-06-10)

Proximity-gated POI commenting is now implemented and verified via a green
`assembleDebug` + 8 passing unit tests. (Live UI tap-through on the emulator was
blocked by a map-tile rendering quirk — see note below — so visual confirmation
of the screen is pending a real-device run.)

- [x] **End-to-end flow** wired and unit-tested (load comments / add / delete +
      gating logic). Live device tap-through deferred (emulator map issue).
- [x] **5km proximity rule** implemented client-side: new `LocationProvider`
      (FusedLocation) + pure `distanceMeters` (Haversine) +
      `Constants.PROXIMITY_RADIUS_METERS`; VM computes distance and gates the
      composer. *Server-side enforcement remains Phase 3.*
- [x] **Comment-ownership check** — verified *already correct*: `Comment.username`
      stores the user's email, so `username == currentUserEmail` is email-to-email.
      Left as-is; the proper stable-`userId` refactor is folded into Phase 3
      (security rules need a uid anyway).
- [x] **Rate-limiter fixed** — now shows a snackbar when exceeded (was a silent
      no-op).
- [x] **PoiDetailScreen cleaned up** — removed the rambling `LaunchedEffect`
      comments; extracted a `CommentComposer`; split terminal load-error from
      transient action errors (failed add/delete now snackbar via a `uiEvent`
      channel instead of wiping the whole screen).
- [x] **Strings extracted** to `strings.xml` for the POI feature surface
      (detail screen + `CommentCard`). App-wide i18n remains a broader effort.

> **Bonus done:** guests are now correctly gated out of commenting with a
> "Sign in to join the conversation" prompt (the repo already rejected anonymous
> comments; the UI now reflects it instead of failing on send).
>
> **Finding (not Phase 1):** the Map screen renders blank tiles on the
> Pixel_9_Pro emulator despite a valid API key (no auth-failure in logs, Firestore
> works) — most likely an emulator rendering quirk; verify on a real device. This
> is the only entry point to POI detail, so it also blocked live UI verification.

---

## 🧹 Phase 2 — Structural cleanup (dead code & drift) — ✅ COMPLETE (2026-06-10)

Decision: **keep the documented `ui/` + `domain/usecase/` layout** (architecture.md
is the source of truth). Remove the competing half-migration and dead code.
*(Agent scaffolding — `GEMINI.md`, `.agent/` — is intentionally kept.)*

- [x] **Collapse the `feature/` experiment.** Moved
      `feature/map/presentation/{PoiDetailScreen,PoiDetailViewModel,PoiDetailUiState}.kt`
      (+ `PoiDetailUiStateTest`) into a new `ui/poi/` package (matches the
      per-screen `ui/post/`, `ui/feed/` convention); updated package, the
      `NavGraph` reference, and architecture.md's file tree + nav diagram;
      deleted `feature/`.
- [x] **Delete orphaned duplicate use cases:**
      `feature/map/domain/{AddPoiCommentUseCase,GetPoiCommentsUseCase}.kt`
      removed; the live `domain/usecase/comment/` copies kept.
- [x] **Delete orphaned `ui/map/PoiDetailUiState.kt`** — removed (file was
      effectively empty).
- [x] **Delete the legacy `models/` package** (`Comment.kt`,
      `PointsOfInterest.kt`, `Post.kt`) — removed; nothing outside the package
      referenced them.
- [x] **Reconcile the agent docs.** Updated `GEMINI.md` to point at the real
      `architecture.md` and at `ROADMAP.md` (in place of the nonexistent
      `DEVLOG.md`) rather than creating a new doc to maintain.
- [x] **Re-verify build is green** — `assembleDebug` + `testDebugUnitTest`
      both pass (20/20 tests, incl. the relocated `PoiDetailUiStateTest`).

> **Leftover drift (resolved):** `MapScreen`, `MapViewModel`, and `MapUiState`
> declared the singular package `com.example.echo.ui.map` despite living in the
> `ui/maps/` directory. Aligned all three to `ui.maps` (plus the `NavGraph`
> import); the whole `ui/` tree now has matching package/directory names.

---

## 🔒 Phase 3 — Production hardening (the real Play Store work)

These are the items that separate a demo from a publishable app.

### Security & data integrity
- [x] **Stable `authorId` (uid) refactor** *(prerequisite, 2026-06-12).* Posts
      and comments now persist the author's Firebase `uid` as `authorId` (set at
      create time), threaded through entities → mappers → domain models. This is
      what makes non-spoofable ownership rules possible (email alone is mutable
      and was the old ownership key). POI comment delete in the UI now checks
      `authorId == uid` instead of email. `toggleLike` was made a single atomic
      write so `likeCount == likes.size()` can be enforced.
- [x] **Firestore Security Rules** *(authored, emulator-validated, and
      **DEPLOYED** to `echo-2b5ba` 2026-06-12; `firestore.rules`).* Reads require
      sign-in; creates require non-anonymous auth + `authorId ==
      request.auth.uid` + `username == token.email` (no spoofing) + field
      validation; posts/comments are deletable only by their author; comments
      are immutable; POIs are read-only except a ±1 `commentCount` move; likes
      restricted to adding/removing **self**. The cloud compiled and released
      the rules; they are now enforcing in production. `firebase.json`,
      `.firebaserc`, `firestore.indexes.json` are in place for future deploys
      (`firebase deploy --only firestore:rules`).
      *Update (2026-06-12): tightened all post/POI **write** guards to
      `isNotAnonymous()` so guests can read but cannot like/edit/delete (a guest
      could previously toggle likes); re-deployed.*
- [x] **Storage rules** *(authored 2026-06-12; `storage.rules`).* The app has
      **no Storage feature yet** (no client uploads/downloads), so the bucket is
      locked down **deny-all**. **⚠ Not deployed:** Firebase Storage has not been
      provisioned on `echo-2b5ba` (no "Get Started" in the console), so there's
      nothing to deploy to and nothing using it. When image upload lands:
      provision Storage, swap in auth-scoped owner-path + size/type rules, then
      `firebase deploy --only storage`.
- [ ] **Server-side proximity & rate limiting** *(deferred to a Cloud Function —
      cannot be done in rules).* Firestore rules have **no trusted source** for
      the caller's physical location, and per-user time-window rate limits need
      server state, so neither the 5km rule nor abuse limits can be enforced in
      `firestore.rules`. Both remain **client-side-gated only** until a Cloud
      Function (or App Check + callable that derives/validates location
      server-side) is added. This is the one genuinely unfinished security item.

### Robustness
- [x] **Guest write-gating fixed** *(2026-06-12).* Guests could like posts (and
      the Create Post FAB showed for them). Now: guests are blocked from liking
      in the repo + rules, the Feed shows a "Sign in to like posts" snackbar
      instead of a silent no-op, and the FAB is hidden for guests.
- [x] **Auth back-stack fixed** *(2026-06-12).* After sign-**up**, the sign-in
      screen was left under Feed, so Back from Feed returned to it. Both SignIn
      and SignUp now `popUpTo(findStartDestination, inclusive)` so Feed is the
      root and Back exits the app.
- [ ] **Auth edge cases**: account-exists-with-different-credential, expired
      sessions, Google sign-in cancel, network loss, password-reset failures.
- [ ] **Consistent loading / empty / error states** across Feed, Map, Profile,
      PostDetail, PoiDetail (some screens have them, others don't).
- [ ] **Location permission UX**: runtime request, rationale, and a graceful
      degraded mode when the user denies location. *(Partial: Create Post now
      shows "location unavailable" gracefully, but still doesn't **request**
      permission at runtime.)*
- [ ] **Crash & analytics**: wire up Firebase Crashlytics.

> **Finding (2026-06-13) — writes hang silently on a token-refresh failure
> (recommended next item).** After ~8h signed in, the 1-hour Firebase ID token
> expired and couldn't refresh: `securetoken.googleapis.com … GrantToken are
> blocked` → Firestore writes failed `UNAUTHENTICATED` and **retried forever**
> (the Post button just spun — no error, no timeout). *Root cause (resolved,
> console-side):* the Android API key's **API restrictions** allowed Identity
> Toolkit (sign-in) but not the **Token Service API** (refresh); fixed by removing
> the redundant API restriction (the key already restricts by Android app +
> SHA-1). Re-verified on-device: token refresh works, the stuck write flushed,
> and new writes (post/like) succeed. **Still open — app-side:** writes should
> **time out + surface an error**, and an expired/invalid session should prompt
> re-login, instead of spinning forever. This is the concrete, observed instance
> of the "expired sessions / network loss / consistent error states" items above
> and is the recommended next thing to build. ⚠ **Before launch:** also register
> the **release / Play App Signing upload SHA-1** on the key, or production hits
> the same wall the first time a token needs refreshing.
>
> **Partly addressed (2026-06-13):** added `withWriteTimeout` ([WriteTimeout.kt](app/src/main/java/com/example/echo/data/WriteTimeout.kt))
> wrapping **all post/comment writes** (create/update/delete/like/comment) — a
> hung write now fails after 10s with a retryable "Couldn't reach the server"
> error instead of spinning forever; `FeedViewModel` now surfaces like failures
> via a snackbar (it previously swallowed them). Verified: no regression on
> normal writes, and offline writes still resolve locally (no false timeout).
> **Still open:** detect an expired/invalid session and **prompt re-login** (vs. a
> generic error), and extend error-surfacing to the remaining VMs (`ProfileViewModel`
> edit/delete and `PostDetailViewModel` comments still swallow failures).

> **Manual verification (2026-06-12, Pixel_9_Pro emulator).** Drove the app
> end-to-end against the live rules: feed reads, like toggle (both directions),
> guest-blocked create, authed create-post, post-comment add, POI comment
> add/delete (proximity-gated, in range), and session persistence — all pass,
> no `PERMISSION_DENIED`. Two environment notes: (1) the Google **Map screen
> ANRs under software GPU** rendering (`Input dispatching timed out`) — heavy
> map render on the emulator, not a logic bug; recheck on a real device. (2)
> The map renders blank tiles under *host* GPU but fine under `swiftshader`.

### UI completeness
- [x] **Post edit/delete wired** *(done in the UI Rebrand, 2026-06-13).* Profile
      posts now have a "⋮" menu → **Edit** (prefilled dialog) / **Delete**
      (confirm), calling the existing `ProfileViewModel.updatePost`/`deletePost`.
      The "⋮" is an optional `PostCard` affordance, so the Feed stays clean. See
      the **UI Rebrand** section below.
- [ ] **Post-comment delete still unwired** — `PostDetailScreen` renders
      `CommentCard(comment)` without an `onDelete` (POI comments have it; post
      comments don't). Small follow-up: pass an owner-gated delete + a
      `PostDetailViewModel` delete method.

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

## 🎨 Phase 3.5 — Neighborhood UI Rebrand — ✅ COMPLETE (2026-06-13)

A full visual rebrand + brand identity for Echo, designed in **Stitch** (Google)
and built into the app. Direction chosen: warm, **light-first "Neighborhood"**
(coral primary, teal secondary, sunny-yellow accent, cream canvas); audience =
young urban folks + neighbors of all ages. Stitch project:
`projects/11588814282991010539`. Every step was verified on the Pixel_9_Pro
emulator (swiftshader) and committed/pushed to `master`.

- [x] **Design system → Compose tokens.** New warm palette, rounded soft shapes,
      and a **Newsreader** (serif headlines) + **Plus Jakarta Sans** (body) type
      scale — bundled as variable TTFs in `res/font` (downloadable Google Fonts
      were dropped: the certs resource isn't shipped by the deps).
      ([Color.kt](app/src/main/java/com/example/echo/ui/theme/Color.kt),
      [Type.kt](app/src/main/java/com/example/echo/ui/theme/Type.kt),
      [Shape.kt](app/src/main/java/com/example/echo/ui/theme/Shape.kt),
      [Theme.kt](app/src/main/java/com/example/echo/ui/theme/Theme.kt))
- [x] **Brand mark + launch.** New Echo "ping" logo (concentric coral/teal ripple
      rings) as a vector ([echo_logo.xml](app/src/main/res/drawable/echo_logo.xml));
      rebranded the adaptive **launcher icon** (cream bg) and the system + in-app
      **splash** (cream, replacing the old blue radar-pin); reused on Sign In.
      *(API 24–25 still fall back to the old webp icon — regenerate later if those
      versions matter.)*
- [x] **Reusable components:** `AuthorAvatar` (initials), `EchoTagChip`,
      reskinned `PostCard` (warm card, avatar header, heart-style like, optional
      "⋮" edit/delete menu, optional distance badge) and `CommentCard`.
- [x] **All screens reskinned:** Feed, Sign In, Sign Up, Create Post, Profile,
      POI Detail, Post Detail, Splash. Bottom nav gained a **center Create**
      button (the old corner FAB was removed).
- [x] **Distance badges** — each located post shows a teal "X m away · time" pill
      (the signature hyperlocal touch). `FeedViewModel` fetches the user's location
      via `LocationProvider`; `LocationProviderImpl` switched to **HIGH_ACCURACY**
      GPS (balanced/network returned null on a cold start, so no badges showed).
- [x] **Create Post location reliability** — moved off FusedLocation's null-prone
      `lastLocation` (read directly in the composable) to `LocationProvider`
      (high-accuracy) in the ViewModel, with "getting location / attached /
      unavailable" status and Post disabled while resolving. Verified to attach a
      fix on a **cold launch** (the old path returned null there).

> **Profile edit/delete** (the original "UI completeness" item) was completed as
> part of this rebrand — see Phase 3 → UI completeness.

---

## 🛍️ Phase 4 — Store listing & compliance

- [ ] **Privacy Policy** (required — the app uses location + auth + user data).
- [ ] **Play Data Safety form** declaring location, email, and content data.
- [ ] **Store assets**: app icon, feature graphic, phone/tablet screenshots,
      short + full description.
- [ ] **Refresh README media** — the `docs/gifs/*` referenced in README should
      reflect the **rebranded Neighborhood UI** (new logo, coral/cream theme,
      distance badges, center-Create nav), not the old navy look.
- [ ] **App content rating** questionnaire + target audience declaration.

---

## 💡 Phase 5 — Post-launch / nice-to-have features

Deferred until shipped; captured so they aren't lost.

### Social & Engagement
- [ ] Push notifications (replies, nearby activity) via FCM.
- [ ] Post editing/deletion polish + report/block (moderation — may become
      required depending on Play policy for UGC apps).
- [ ] Direct messaging or threaded replies to comments.
- [ ] Follow/friend nearby users.
- [ ] Bookmarks/saved posts.

### Map & Location
- [ ] Richer map: clustering tuning, search, "recenter on me".
- [ ] GeoFire interaction radius limit — restrict which posts/POIs a user can
      see or interact with based on a configurable distance from their current
      location (extend the existing 5km comment-proximity rule to broader
      feed/map visibility).
- [ ] Geofenced alerts ("something new posted near you").
- [ ] User-submitted POIs (vs. only the seeded list).
- [ ] Location link on each post — tapping a post opens the map centered on
      that post's location (deep link from PostDetail → MapScreen).

### Content
- [ ] Multi-photo posts / image carousel.
- [ ] Event-type posts with RSVP/date-time.
- [ ] Post expiration after 24 hours — auto-hide or delete posts older than
      24h, possibly via a scheduled Cloud Function.

### Profile & Account
- [ ] Avatar upload, bio/display name.
- [ ] Post history with stats (total likes/comments).
- [ ] Settings screen (notification prefs, dark mode toggle, account deletion).

### Technical
- [ ] Offline cache / Room layer for feed resilience.
- [ ] Search/filter persistence across sessions.

---

### Suggested order of attack
**Phases 0–2, the Phase 3 security/data-integrity block, and the 3.5 UI Rebrand
are ✅ done.** Remaining path to launch:
1. **Finish Phase 3 Robustness** — start with the observed **write / expired-
   session failure handling** (see the 2026-06-13 finding: writes should time out
   + surface an error and prompt re-login, not spin forever), then Crashlytics,
   consistent loading/error states, and runtime location-permission UX.
2. **Phase 3 Build & release** — R8 + keep rules, release signing, a signed AAB,
   edge-to-edge / target API, and remove the `PoiRepository` debug logging. This
   is the hard gate to actually publishing.
3. **Phase 4 store prep** — privacy policy, Data Safety form, app content rating,
   and fresh screenshots (which can now show off the new UI).

**Still open from earlier phases:** server-side proximity & rate limiting (needs a
Cloud Function), post-comment delete UI, and the test harness. **Phase 5** is
post-launch.
