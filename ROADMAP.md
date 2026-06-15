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
- **Server-side proximity & rate limiting** — *cannot be done in `firestore.rules`*:
  rules have **no trusted source** for the caller's physical location, and per-user
  time-window rate limits need server state. Both protections (the 5km comment rule
  and the spam limiter) are **client-side-gated only** today, so anyone hitting the
  database directly — bypassing the app — skips them. Like Tinder et al., this can
  never be made *perfect* (any client-reported location can be spoofed); the goal is
  to make bypass expensive and detectable. Address it as a staircase, cheapest win
  first:

  - [x] **Step 1 — App Check / Play Integrity.** Biggest bang-for-buck. The OS
        cryptographically attests each request came from the genuine, unmodified app
        on a real device, which blocks the common attack (curl/bots/modified APK
        hitting the API directly). *Implemented in `EchoApplication`* (Play Integrity
        for release, debug provider locally). **Remaining (console-side):** register
        the app under App Check, add the debug token from logcat, then flip
        enforcement ON for Firestore (and Functions, once Step 2 lands).
  - [ ] **Step 2 — Server-side distance check in a callable Cloud Function.** Move
        the 5km rule off the phone: the client calls a function that fetches the
        POI's coordinates from Firestore (the *trusted* source), validates distance,
        enforces a per-user rate limit via a Firestore counter, and only then writes
        the comment via the Admin SDK. Rules then forbid direct client comment
        creates. A spoofer can no longer bypass the check by editing the APK.
        **Prerequisite: the Blaze (pay-as-you-go) plan** — Cloud Functions don't run
        on the free Spark plan (generous free tier; effectively free at this volume).
  - [ ] **Step 3 — Cross-check location signals** *(later, once there are real users
        worth abusing).* Corroborate the reported GPS against IP geolocation, the
        Android mock-location flag (`isFromMockProvider`), and "can't teleport"
        velocity checks. Defeats a spoofer feeding fake GPS to the genuine app —
        the one gap Steps 1–2 leave open. Luxury tier; not needed for launch.

### Robustness
- [x] **Guest write-gating fixed** *(2026-06-12).* Guests could like posts (and
      the Create Post FAB showed for them). Now: guests are blocked from liking
      in the repo + rules, the Feed shows a "Sign in to like posts" snackbar
      instead of a silent no-op, and the FAB is hidden for guests.
- [x] **Auth back-stack fixed** *(2026-06-12).* After sign-**up**, the sign-in
      screen was left under Feed, so Back from Feed returned to it. Both SignIn
      and SignUp now `popUpTo(findStartDestination, inclusive)` so Feed is the
      root and Back exits the app.
- [~] **Auth edge cases**: **expired sessions ✅** (2026-06-13 — reactive
      `AuthRepository.authState()` / `AuthStateListener` drives
      `AuthViewModel.currentUser`, so RootNavHost routes back to Sign In on
      session loss). Still open: account-exists-with-different-credential,
      Google sign-in cancel, network loss, password-reset failures.
- [x] **Consistent loading / empty / error states** *(2026-06-13).* Shared
      `EmptyState` (icon + title + subtitle, error tint) and shimmer skeletons
      (`Shimmer` / `PostCardSkeleton` / `AlertCardSkeleton`) now back Feed,
      Profile, Alerts, and PostDetail (empty + error). Map and PoiDetail use
      inline states.
- [x] **Location permission UX** *(2026-06-13).* In-context runtime request with
      a rationale: toggling "Share your location" on **Create Post** now shows a
      rationale dialog → system request, with an "Open Settings" path when
      permanently denied; location is only enabled once granted. **Map** already
      requests at runtime (with a "Grant Permission" screen), and **Feed**
      degrades gracefully (no distance badges without permission).
- [x] **Crash reporting**: Firebase Crashlytics wired *(2026-06-14)* — Gradle
      plugin + SDK added; `EchoApplication` sets
      `isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG` so only **release**
      builds report (dev crashes don't pollute the dashboard). Verified on-device:
      Firebase initializes and the Crashlytics dependency registers
      (`FirebaseSessions: Dependency to CRASHLYTICS added`). *Remaining: confirm a
      real release crash lands in the console, and (optional) upload mapping files
      once R8 is enabled.*

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
> **Resolved (2026-06-13):** expired/invalid sessions now **prompt re-login** — a
> reactive `AuthStateListener` (`AuthRepository.authState()`) drives
> `AuthViewModel.currentUser`, so RootNavHost routes back to Sign In on session
> loss. Error-surfacing was extended to the remaining VMs: `ProfileViewModel`
> (edit/delete/like) and `PostDetailViewModel` / `PoiDetailViewModel` (comments)
> now route failures to snackbars via a `uiEvent` channel instead of swallowing
> them.

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
- [x] **Post-comment delete wired** *(2026-06-13).* New `DeleteCommentUseCase`;
      `PostDetailViewModel` exposes `currentUserId` + a `deleteComment` method,
      and `PostDetailScreen` shows the trash affordance only on the viewer's own
      comments (matching the POI pattern). Ownership is also enforced by the
      Firestore rules.

### Build & release
- [x] **R8/ProGuard** *(2026-06-14).* `isMinifyEnabled` + `isShrinkResources`
      on for release; keep rules added for the Firestore entities
      (`data.entity.**`, deserialized reflectively via `toObject`) plus
      `SourceFile`/`LineNumberTable` for readable Crashlytics traces.
      **Verified:** `assembleRelease` builds and the minified APK **runs on
      device** — Feed loads (proves Firestore reflection + Hilt + Compose survive
      shrinking). *Two release-only bugs were caught & fixed in the process:* the
      App Check **debug** provider was referenced from `main` (a `debugImplementation`
      class) → split into `src/debug` + `src/release` `installAppCheck()`; and the
      Crashlytics auto **mapping-file upload** failed offline → disabled
      (`mappingFileUploadEnabled = false`; upload explicitly when publishing).
- [x] **Release signing + signed AAB** *(2026-06-14).* Upload keystore created
      (`echo-upload.jks`, alias `echo-upload`); a gitignored `keystore.properties`
      (template: `keystore.properties.example`) drives the release `signingConfig`.
      **`./gradlew bundleRelease` produces a signed, R8-minified AAB**
      (`app/build/outputs/bundle/release/app-release.aab`) — verified with
      `jarsigner -verify` → "jar verified". Release SHA-1
      `86:B0:45:…:37:4D` / SHA-256 `B9:F9:1F:…:8D:CC`. **Still to do at publish
      time:** enrol in **Play App Signing** when creating the listing, and
      register the release (and Play app-signing) fingerprints on the Maps/API
      key + App Check.
- [x] **Target API + edge-to-edge verified** *(2026-06-14).* `targetSdk`/
      `compileSdk = 35` (above Play's min-34 requirement). `enableEdgeToEdge()`
      is called and there are **no deprecated inset APIs** (`statusBarColor`,
      `SYSTEM_UI_FLAG`, etc.); `themes.xml` has no system-bar color overrides.
      Confirmed on-device: M3 `TopAppBar`s consume the status-bar inset and the
      bottom nav uses `navigationBarsPadding()`, so system bars never obscure
      content (top bar draws under the status bar by design, nav sits above the
      gesture pill).
- [x] **Remove debug logging** in `PoiRepository` *(2026-06-13)* — the verbose
      `Log.d/w/e` POI-listener block (added in cd5f797) is gone; parse failures
      now fall through to `null` silently.

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
- [~] **In-app Alerts — v1 shipped.** The `Alerts` tab + `AlertsScreen` /
      `AlertsViewModel` (`ui/alerts/`) surface a feed of engagement (likes /
      comments) on the user's *own* posts, derived from existing post data — no
      notifications backend. Tapping an alert opens that post. **Enrichment path
      (all behind this same UI):**
  - [ ] **Per-event alerts** ("Sarah commented on your post") instead of the
        current post-level rollup ("1 like on your echo"). Needs either an
        `events`/`notifications` collection written on like/comment, or a
        client-side diff of comment streams.
  - [ ] **Proximity alerts** ("something new posted near you") — feed the Alerts
        screen from recent posts within the user's radius (reuses
        `LocationProvider` + `distanceMeters`); see *Geofenced alerts* below.
  - [ ] **Unread state** — track last-seen timestamp to badge the Alerts tab and
        distinguish new vs. seen.
- [ ] Push notifications (replies, nearby activity) via FCM — would mirror the
      in-app Alerts feed above to the system tray.
- [ ] Post editing/deletion polish + report/block (moderation — may become
      required depending on Play policy for UGC apps).
- [ ] Direct messaging or threaded replies to comments.
- [ ] Follow/friend nearby users.
- [ ] Bookmarks/saved posts.

### Map & Location
- [~] Richer map: **"recenter on me" ✅** (bottom-right FAB, 2026-06-13) + a
      floating search/filter bar over a full-screen map. Still want clustering
      tuning and real free-text search (the bar currently opens the tag filter).
- [ ] GeoFire interaction radius limit — restrict which posts/POIs a user can
      see or interact with based on a configurable distance from their current
      location (extend the existing 5km comment-proximity rule to broader
      feed/map visibility).
- [ ] Geofenced alerts ("something new posted near you") — would populate the
      existing **Alerts** screen (see *In-app Alerts* under Social & Engagement)
      rather than a new surface.
- [ ] User-submitted POIs (vs. only the seeded list).
- [ ] Location link on each post — tapping a post opens the map centered on
      that post's location (deep link from PostDetail → MapScreen).

### Content
- [ ] Multi-photo posts / image carousel.
- [ ] Event-type posts with RSVP/date-time.
- [ ] Post expiration after 24 hours — auto-hide or delete posts older than
      24h, possibly via a scheduled Cloud Function.

### Profile & Account
- [x] **Usernames + real names** *(2026-06-14).* Multi-step sign-up: after the
      email/password step, a **Complete Profile** screen collects first/last name
      and a unique **@username** (live availability check). Backed by
      `users/{uid}` + a `usernames/{handle}` reservation (atomic transaction;
      rules enforce uniqueness + format + owner-write). A completion gate routes
      profile-less accounts there on launch. New posts/comments use the @handle
      (validated by rules via `get(users/uid).username`); old content is left
      as-is. Profile shows "First Last" + @handle; own-posts query by uid.
      Verified end-to-end on device against deployed rules.
- [ ] **SMS 2FA / phone verification** — *deferred:* requires the Blaze plan +
      per-SMS cost (and Identity Platform for true MFA). The anti-bad-actor goal
      is largely met for free by App Check + Play Integrity (already in the app).
      Revisit if/when Blaze is on the table.
- [x] **Bio** *(2026-06-14).* Added a `bio` field (≤160 chars) to the profile
      schema (`UserProfile` + `users/{uid}`). A new **Edit profile** screen
      (coral top bar, reachable from the Profile top-bar pencil) edits first/last
      name + bio with a live character counter; `@username` is shown read-only
      (handles are permanent). Rules: `users create` now requires `bio`, and a
      new owner-only `users update` permits changing only `firstName/lastName/bio`.
      ⚠️ **Requires the firestore.rules deploy** — the create rule now lists `bio`
      in `hasOnly`, so the app update and `firebase deploy --only firestore:rules`
      must ship together or new sign-ups break.
- [~] Avatar upload — *blocked on the free (Spark) plan:* image uploads need
      Firebase **Storage**, which requires Blaze. The Edit profile screen keeps
      the initials avatar with a "Photo uploads coming soon" caption as the
      placeholder. Revisit if/when Blaze is on the table (same gate as SMS 2FA).
- [x] **Post history with stats** *(already shipped).* The Profile screen shows
      Posts / total Likes / total Comments stat tiles plus the full post list
      (`ProfileViewModel` sums `likeCount`/`commentCount` across the user's posts).
- [x] **Settings screen** *(2026-06-14).* Reachable from the Profile top-bar gear.
      Sections: **Appearance** (dark-mode toggle — warm dark theme, persisted via
      Jetpack DataStore and applied app-wide through `EchoTheme(darkTheme=…)`);
      **Notifications** (a persisted preference toggle, stored for when FCM lands);
      **Account** (Sign out; Delete account). Deletion **re-authenticates first**
      (a "Confirm it's you" password dialog for email accounts) and only then
      deletes the `usernames/{handle}` reservation + `users/{uid}` doc + the Auth
      user — so a stale session or wrong password destroys nothing. Verified
      end-to-end on device against deployed rules.
- [ ] **Audit the account-deletion flow for orphaned sessions** *(tabled
      2026-06-15).* Found a device in an **authenticated-but-profileless** state:
      a valid Firebase Auth session with no `users/{uid}` doc, which
      `RootNavHost` routes to **Complete Profile**. That screen was a hard
      dead-end (no back/sign-out; it's the start destination, so system-back
      closed the app and relaunch returned there). **Fixed the trap** by adding a
      **Sign out** action to the Complete Profile top bar
      ([CompleteProfileScreen.kt](app/src/main/java/com/example/echo/ui/auth/CompleteProfileScreen.kt)) —
      verified on-device (Sign out → Sign In). **Still to investigate:** how the
      orphaned state is produced — if deletion can wipe the profile doc but leave
      the Auth user alive (re-auth path, partial failure), it recreates this trap.
      Confirm deletion is atomic/ordered so the Auth user goes last, and consider
      a guard in routing for the profileless-session case.
- [ ] **Google account deletion + Google Sign-In config.**
      **Debug sign-in now wired up (2026-06-15):** debug SHA-1 registered in
      Firebase, `google-services.json` re-downloaded (now has a type-3 web OAuth
      client), and `MainActivity` reads the id from `R.string.default_web_client_id`
      instead of the old `"YOUR_WEB_CLIENT_ID"` placeholder. "Continue with Google"
      should work in debug builds. **Tabled for later (before Play Store):**
      - Add the **Play App Signing SHA-1** (Play Console → Setup → App signing) to
        Firebase, or Google Sign-In breaks for published builds.
      - Verify the **Google account-deletion re-auth path** now that sign-in works
        (it previously showed a "not available yet" message). Google requires a
        working account-deletion path before Play Store.
      - Add the upload-key SHA-256 to Firebase (good hygiene; needed for Credential
        Manager / App Check Play Integrity if adopted later).

### Technical
- [ ] Offline cache / Room layer for feed resilience.
- [ ] Search/filter persistence across sessions.
- [ ] **Backfill POI photos (2026-06-15).** POI detail now shows a curated `imageUrl`
      with a type-icon fallback when absent. Add an `imageUrl` per POI in the seeding
      script `C:\Users\Paul\OneDrive\Desktop\Echo\seed_pois.py` (prefer free Wikimedia
      Commons URLs — no hosting, clear licensing) and re-run it. Until then every POI
      shows the type-icon fallback. Also wire up the Share button on the detail screen.
- [ ] **Map performance & Firestore cost audit (2026-06-15).** Found during a map
      sanity check. The marker-tap jank (ripple animation recomposing every marker
      ~60fps) is **fixed**; these remain:
      - **[High] Unbounded posts read.** `PostRepositoryImpl.getPosts()` attaches a
        live snapshot listener to the whole `posts` collection — no `.limit()`, no
        geo bound. Every map/feed open bills a read for every post and re-reads on
        every change. On the Spark plan (50K reads/day) this caps sessions and grows
        with the collection forever. Stopgap: `.limit(N)` newest posts. Proper fix:
        geohash-based geo queries (bigger change).
      - **[High] Duplicate full-collection listeners.** MapViewModel and FeedViewModel
        each open their own `getPosts()` listener (one `callbackFlow` per collector);
        with `WhileSubscribed(5000)` they overlap on tab switches → ~2× reads. Share a
        single upstream in the repo (e.g. `shareIn`).
      - **[High] Over-fetch then discard.** Map downloads all posts then filters out
        location-less ones client-side — paying to download posts it never plots.
      - **[Med] O(n²) clustering re-runs on unrelated changes.** `clusterPosts` sits
        inside the 8-way `combine`, so tapping a marker re-clusters every post even
        though data didn't change. Split clustering (data→clusters) from selection.
      - **[Med] Continuous zoom spam.** `LaunchedEffect(zoom)` pushes every fractional
        zoom delta into `_currentZoom` → full combine + O(n²) re-cluster, but the
        cluster radius only changes at zoom buckets 13/15/17. Bucket zoom +
        `distinctUntilChanged`.
      - **[Low]** Splitting the combine also removes the fragile `Array<Any?>` +
        unchecked casts. Cache marker icon bitmaps by `(resId, scale)` to avoid
        re-rasterizing identical icons per marker.

---

### Suggested order of attack
**Phases 0–2, the Phase 3 security/data-integrity block, and the 3.5 UI Rebrand
are ✅ done.** Remaining path to launch:
1. **Finish Phase 3 Robustness** — write timeouts ✅, expired-session re-login ✅,
   loading/empty/error states ✅, location-permission UX ✅, post-comment delete ✅,
   and Crashlytics ✅ are now done. Remaining: the lower-priority **auth edge
   cases** (account-exists-with-different-credential, Google cancel, network
   loss, password-reset failures).
2. **Phase 3 Build & release** — R8 + keep rules, release signing, a signed AAB,
   edge-to-edge / target API, and remove the `PoiRepository` debug logging. This
   is the hard gate to actually publishing.
3. **Phase 4 store prep** — privacy policy, Data Safety form, app content rating,
   and fresh screenshots (which can now show off the new UI).

**Still open from earlier phases:** server-side proximity & rate limiting — **Step 1
(App Check) done**; Step 2 (server-side distance check via callable Cloud Function)
needs the Blaze plan; Step 3 (cross-check signals) is post-launch luxury. Also
post-comment delete UI and the test harness. **Phase 5** is post-launch.
