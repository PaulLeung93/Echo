# Echo — Launch Status
**Goal:** Ship to Google Play Store
**Last updated:** 2026-06-22

---

## ✅ Completed (summary)

All core development phases are done. Key milestones:

- **Build & architecture** — green build, dead code removed, package renamed `dev.echoapp.echo`
- **Features** — Feed, Map (geohash-bounded), POI post threads, Profile, Alerts, Create Post, Post Detail, Settings, Dark mode, Avatar upload, Offline-first Room cache
- **Security** — Firestore + Storage rules deployed, `authorId` ownership, App Check (code side), guest write-gating, write timeouts
- **UI** — Full Neighborhood rebrand (coral/cream, Newsreader + Jakarta Sans, distance badges, center FAB)
- **Release build** — R8/ProGuard, signed AAB verified (`echo-upload.jks`), edge-to-edge / targetSdk 35
- **QA fixes** — app ID renamed, email removed from public profile, Google account deletion wired, robustness fixes (transactions, shared flows, batched writes, lifecycle-aware collection), layer cleanup
- **Firebase** — rules + indexes + `onPostDeleted` Cloud Function deployed; geohash backfill run (28 posts); profile emails stripped from Firestore
- **Store prep** — privacy policy + deletion page live at `https://echo-2b5ba.web.app`; 5 screenshots captured in `docs/screenshots/`; Data Safety + Content Rating answers documented in `docs/launch/`

---

## 🚧 Remaining — before Play Store submission

### 1. Play Store account (blocked on you)
- [ ] Complete **identity verification** in the Google Play Console — required before creating a listing

### 2. App Check — ✅ Done (confirm in Firebase Console if unsure)
Firebase Console → App Check → Apps → Echo → Firestore should show **"Enforced"**.

### 3. Report / flag content mechanism ⚠️
- [ ] Play policy requires a way to **report objectionable content or users** for any app with UGC + user-to-user communication. Echo has **block** (Settings) but no **report** button.
- This may be a hard requirement before the listing is approved. Confirm during review — if rejected, add a report flag on posts/comments.

### 4. Play Console listing (once account is verified)
- [ ] **Feature graphic** — 1024×500px banner image (the one asset not yet created)
- [ ] **App description** — short (≤80 chars) + full (≤4000 chars)
- [ ] Upload **screenshots** from `docs/screenshots/` (5 ready)
- [ ] **Privacy policy URL** → `https://echo-2b5ba.web.app/privacy.html`
- [ ] **Data Safety form** → answers in `docs/launch/DATA_SAFETY.md`
- [ ] **Content Rating questionnaire** → answers in `docs/launch/CONTENT_RATING.md`
- [ ] **Enroll in Play App Signing** → copy the Play App Signing SHA-1 from Play Console → Setup → App signing → add it to Firebase (Authentication → Android app) → re-download `google-services.json` and rebuild. Without this, Google Sign-In breaks for all Play Store users.

### 5. Test harness (recommended before submission)
- [ ] Unit tests for use cases / mappers, ViewModel tests, a couple of integration tests
- Use the `testing-setup` skill to scaffold

---

## 🟡 Nice-to-have before launch

- **Auth edge cases** — account-exists-with-different-credential, Google cancel, network-loss during auth, password-reset failures
- **Orphaned-session audit** — confirm account deletion is atomic (Auth user deleted last); the escape hatch (Sign out on Complete Profile) is already fixed
- **Server-side proximity Step 2** — move the 5km POI comment rule into a callable Cloud Function (currently client-side only; Blaze plan is active so this is unblocked)

---

## 💡 Post-launch (Phase 5)

Not needed for submission. Captured so they aren't lost.

- **Alerts enrichment** — per-event alerts ("Sarah commented"), proximity alerts, unread badge
- **Push notifications** (FCM) — mirrors Alerts to the system tray
- **Report / moderation** — report button on posts/comments, abuse policy (may move up to launch requirement — see §3 above)
- **Map** — clustering tuning, real free-text search (current bar opens tag filter)
- **POI activity glow** — ✅ implemented: a POI shows a gold glow when it has an echo from the last 1h that the user hasn't seen yet; the glow clears once they open its thread and re-lights if a newer echo arrives. (`lastPostAt` denormalized per POI; window in `CreatePinIcon.POI_ACTIVE_WINDOW_MILLIS`; per-user "viewed" times in DataStore, device-local.) A true animated pulse on the *selected* POI remains a possible future polish.
- **GeoFire radius** — cap POI visibility + interactions to a configurable distance
- **Content** — multi-photo posts, event-type posts with RSVP, 24h post expiry
- **Social** — direct messaging, follow/friend nearby users, bookmarks
- **User-submitted POIs**
- **Server-side proximity Step 3** — cross-check GPS vs IP / mock-location flag / velocity (post-launch luxury)
- **SMS 2FA** — deferred (cost); App Check already covers the main anti-abuse goal
- **Admin key rotation** — `scripts/firebase-key.json` is gitignored; rotate if exposure suspected
