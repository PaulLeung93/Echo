# Echo — Follow feature plan

**Status:** Planned (not built). Tracking doc for the social "follow" feature.
**Last updated:** 2026-06-23
**Related:** [LAUNCH_STATUS.md](../LAUNCH_STATUS.md) (Post-launch → Social)

---

## Decisions (locked)

These were settled during the design brainstorm and frame everything below.

| Decision | Choice | Why |
|---|---|---|
| **Model** | **One-way public follow** (Twitter-style), not mutual friend | Echo's posts are already world-readable to any signed-in user, so a private friend-gate adds friction without protecting anything. No request/accept state machine, no inbox — far less pre-launch risk. |
| **Payoff** | **A distance-free "Following" feed** (Tier B) | Echo is proximity-bounded (`PROXIMITY_RADIUS_METERS = 5km`). Following's whole point is to keep up with someone *regardless of distance* — the person-shaped version of favorite POIs. Counts-only (Tier A) isn't worth the surface area. |
| **Scale target** | Built to scale (future-proof), but **ship the simple version first** | The data model (subcollection edges + denormalized counts) is identical whether the feed is built with chunked queries or a materialized timeline. So we ship chunked-query reads now and keep fan-out-on-write as a clean, additive Phase 4 — no rework. |
| **Entry point** | **Tapping any author** (feed, POI thread, comments) opens their public profile | The follow button lives on that profile. A "find people nearby" discovery surface is out of scope for v1. |
| **Alerts ("Sarah posted")** | Deferred (Tier C) | Touches the currently backend-less Alerts system. Revisit after the feed lands. |

### The key insight
"Follow" is cheap to store; the real questions are **what it unlocks** (a feed
beyond the radius) and **the missing prerequisite** (Echo has no way to view
*another* user's profile today). Most of the work is Phase 0 below, not the edge.

---

## Current-state facts the plan depends on

Verified against the codebase on 2026-06-23:

- **No public profile screen exists.** [`ProfileScreen`](../../app/src/main/java/dev/echoapp/echo/ui/profile/ProfileScreen.kt)
  is hardwired to the *current* user (reads `authState.currentUser`, redirects
  anonymous users out). Authors in `PostCard` / `CommentCard` / POI threads are
  not tappable. **This is the prerequisite, and the biggest single chunk.**
- **`Post` and `Comment` both carry `authorId`** (stable Firebase uid) — so every
  author surface can navigate by uid.
- **`GetPostsByAuthorIdUseCase` already exists** — the public profile gets
  "their posts" for free.
- **`UserProfile`** (`users/{uid}`) already holds `blockedUserIds: List<String>`
  and `favorites: Map<String,Long>`. These are arrays/maps *on the user doc*
  because they're small and self-capped. **Follow edges must NOT live on the user
  doc** — follower counts are unbounded (1 MB doc limit + write contention).
- **Block is already mutual** via `UserRepository.observeHiddenUserIds()` (union
  of "I blocked" + "blocked me", queried with `array-contains`). Follow logic
  hooks into this.
- **Cloud Functions are on Blaze**, with `onPostDeleted` + `onUserDeleted`
  cascade functions already in [`functions/index.js`](../../functions/index.js)
  to extend.
- **Firestore rules** tightly scope every `users/{uid}` update to a specific
  key-set (`firstName/lastName/bio`, `photoUrl`, `blockedUserIds`, `favorites`).
  Follow edges go in *subcollections*, which need their own `match` blocks.
- **No `authorId + timestamp` composite index** exists yet — the Following feed
  needs one (see [firestore.indexes.json](../../firestore.indexes.json)).

---

## Data model

```
users/{uid}/following/{targetUid}   { createdAt }   ← people I follow
users/{uid}/followers/{followerUid} { createdAt }   ← people who follow me
users/{uid}                         { ..., followerCount, followingCount }
```

- The client writes **only its own** `following/{targetUid}` entry.
- A Cloud Function **mirrors** the matching `followers/{me}` entry onto the target
  and maintains both denormalized counts. This avoids fragile rules that would let
  a user edit *another* user's document.
- Counts are denormalized so a profile renders without counting a subcollection.

### Why a Cloud-Function mirror (not pure client writes)
A pure-client design would need rules allowing user A to add a doc under user B's
`followers` subcollection. That's writable-by-strangers surface we'd rather not
open. Letting a function (admin, bypasses rules) do the mirror keeps the only
client-writable path "your own `following` list."

---

## Phased plan

### Phase 0 — Public profile screen (prerequisite, heaviest piece) — ✅ built
Implemented 2026-06-23 (compiles; manual QA pending). No follow logic yet;
independently useful. New: `UserProfileScreen`/`UserProfileViewModel`/
`UserProfileUiState`, `ObserveUserProfileUseCase`, `UserRepository.observeProfileById`,
route `user_profile/{uid}`, and `onAuthorClick` on `PostCard`/`CommentCard` wired
from Feed, Post detail, and POI threads. "Report user" (A.6) intentionally deferred
to Phase 1.
- `UserRepository.observeProfileById(uid)` / `getProfileById(uid)` (build on the
  existing `getProfilesByIds`).
- New `UserProfileScreen` + `UserProfileViewModel`; route `user_profile/{uid}`.
  A read-only twin of `ProfileScreen`, reusing `GetPostsByAuthorIdUseCase`.
- Make authors tappable in `PostCard`, `CommentCard`, and POI threads → navigate
  by `authorId`. (Guard against navigating to your *own* uid → just open your
  Profile, or show the same screen without a follow button.)
- Put the **"Report user"** action here — partially closes §3 (report mechanism)
  of LAUNCH_STATUS.

### Phase 1 — Follow edges + counts (core)
- Subcollection schema above + denormalized counts.
- Cloud Function `onFollowWritten` (or create/delete pair): mirror the `followers`
  entry, update `followerCount`/`followingCount`.
- Rules: `isNotAnonymous()` gate; a user may only create/delete their own
  `following/{target}`; `followers` is client-read-only (function-written).
- **Block integration:** blocking severs follows both directions and bars
  re-follow; reuse `observeHiddenUserIds()`.
- Extend `onUserDeleted`: delete the departing user's `following`/`followers`
  edges and fix counterparties' counts.
- `FollowRepository` + use cases: `followUser`, `unfollowUser`,
  `observeIsFollowing(uid)`, `observeFollowerCount`/`observeFollowingCount`.
- Follow/unfollow button + counts on the Phase 0 profile. Guests can't follow.

### Phase 2 — Following feed, chunked-query v1 (the payoff)
- New "Following" tab/segment on the feed.
- Read my `following` ids → chunked `authorId in [...]` queries on `posts` (≤30
  per chunk), merge + sort by `timestamp` client-side. **No geohash filter** —
  this is the distance-free payoff.
- Add the `authorId + timestamp` (DESC) composite index.
- Empty state: "Follow people to see their echoes anywhere."

### Phase 3 — Polish
- Follower / following list screens (paginated).
- Follow counts on your *own* profile.
- **Update [DATA_SAFETY.md](DATA_SAFETY.md)**: Echo now collects a social graph
  (still "Collected, not Shared"; declare under App activity / User IDs as
  appropriate).

### Phase 4 — (deferred, scale-only) Fan-out-on-write timeline
Turn on only when chunked queries strain (≈ hundreds of followees, or hot feeds).
- `feeds/{uid}/timeline/{postId}` maintained by an `onPostCreated` function that
  fans a new post's id to every follower.
- Phase 2's feed swaps its read source to this collection. Pure addition; the
  Phase 1 edge model already supports it.
- Watch-outs when we get here: write amplification, the "celebrity" fan-out
  problem, a one-time backfill, and storage cost.

---

## Appendix A — Phase 0 implementation spec

Detailed spec for the prerequisite: a public, read-only profile screen for any
user, reachable by tapping an author anywhere. **No follow logic in this phase.**
Verified against the code on 2026-06-23.

### A.1 Scope
- **In:** view any user's public profile (avatar, name, @handle, bio, post/like/
  comment stats, their posts); reach it by tapping an author in the feed, POI
  threads, comments, and post detail; moderate from there (report/block) the same
  way you can from a post.
- **Out (later phases):** follow button, follower/following counts & lists, the
  Following feed.
- **Optional in this phase:** a dedicated "Report user" action (needs a small
  rules + model change — see A.6). Reporting individual posts already works from
  the post cards shown on the profile, so this can slip to Phase 1 without losing
  the moderation surface.

### A.2 New files
| File | Purpose |
|---|---|
| `ui/profile/UserProfileScreen.kt` | Read-only profile UI. A trimmed twin of `ProfileScreen` (no edit/settings/favorites; back arrow instead of bottom-nav). |
| `ui/profile/UserProfileViewModel.kt` | `@HiltViewModel`; reads `uid` from `SavedStateHandle`; combines profile + their posts. |
| `ui/profile/UserProfileUiState.kt` | `userProfile`, `userPosts`, `totalLikes`, `totalComments`, `isLoading`, `error`, plus `isSelf`. |
| `domain/usecase/user/ObserveUserProfileUseCase.kt` | `operator fun invoke(uid): Flow<UserProfile?>`. Thin wrapper; `@Inject` constructor (auto-provided, no DI module edit). |

### A.3 Files to edit
| File | Change |
|---|---|
| `utils/Constants.kt` | Add `const val ROUTE_USER_PROFILE = "user_profile"`. |
| `navigation/NavGraph.kt` | Add `USER_PROFILE` to `Destinations`; add a `composable("${USER_PROFILE}/{uid}", args=[uid])` route rendering `UserProfileScreen`. |
| `domain/repository/UserRepository.kt` | Add `fun observeProfileById(uid: String): Flow<UserProfile?>`. |
| `data/repository/UserRepositoryImpl.kt` | Implement it — a `callbackFlow` snapshot listener on `usersCollection.document(uid)`, reusing the existing private `UserProfileEntity.toDomain()` (mirror `observeCurrentUserProfile`, but for an arbitrary uid). |
| `components/PostCard.kt` | Add `onAuthorClick: ((String) -> Unit)? = null`; wrap the avatar + name region in a nested `clickable { onAuthorClick(post.authorId) }` (same nested-consume pattern the location badge already uses, so it doesn't trigger the card's `onClick`). |
| `components/CommentCard.kt` | Add `onAuthorClick: ((String) -> Unit)? = null`; wrap the avatar + name region likewise. |
| `ui/feed/FeedScreen.kt` | Pass `onAuthorClick` → navigate to `user_profile/{authorId}`. |
| `ui/post/PostDetailScreen.kt` | Same, for both the `PostCard` and each `CommentCard`. |
| `ui/poi/PoiDetailScreen.kt` | Same, for its post/comment cards. |
| `ui/profile/ProfileScreen.kt` | Own posts' `PostCard`: tapping your own author is a no-op (pass `null`) — you're already on your profile. |

### A.4 Navigation contract
- Route: `user_profile/{uid}`; `uid` is the stable Firebase `authorId` already on
  every `Post`/`Comment`.
- **Self-view:** render gracefully rather than redirect. `isSelf = (uid ==
  currentUid)` hides report/block (and later the follow button). Optionally guard
  tap sites with `if (authorId != currentUid)` so tapping yourself does nothing —
  cleaner, avoids a redundant screen. Recommend the guard at call sites *and* the
  `isSelf` defence in the screen.
- **Empty/blank authorId:** legacy content can have an empty `authorId`. Don't
  make those authors tappable (`onAuthorClick` only wired when `authorId` is
  non-blank), matching how `PostDetailScreen` already gates report/block.

### A.5 ViewModel shape (mirrors `ProfileViewModel`)
```
val uid = savedStateHandle.get<String>("uid").orEmpty()
val isSelf = uid == getCurrentUserUseCase()?.id
uiState = combine(
    observeUserProfileUseCase(uid),
    getPostsByAuthorIdUseCase(uid),
) { profile, posts -> UserProfileUiState(
       userProfile = profile,
       userPosts = posts,
       totalLikes = posts.sumOf { it.likeCount },
       totalComments = posts.sumOf { it.commentCount },
       isSelf = isSelf,
       isLoading = false,
   ) }
   .catch { emit(UserProfileUiState(error = it.message)) }
   .stateIn(viewModelScope, WhileSubscribed(5000), UserProfileUiState(isLoading = true))
```
Reuse existing moderation actions (`reportPost`, `blockUser`) the same way
`PostDetailViewModel` does, so the profile's post cards keep report/block. No new
DI bindings: the new use case and VM are constructor-injected; the repository is
already bound in `RepositoryModule`.

### A.6 "Report user" — ✅ built + deployed 2026-06-23
Implemented: `ReportType.USER`; the public profile's top-bar overflow has **Report
user** + **Block user**; the `reports` rule now allows `type in ['post','comment','user']`
(deployed to `echo-2b5ba`). This also closes LAUNCH_STATUS §3. Original spec below.

#### (original spec)
The report pipeline today only allows `type in ['post','comment']`
([firestore.rules](../../firestore.rules) `reports` block) and
`ReportType { POST, COMMENT }`. A profile-level "Report user" needs:
- `ReportType.USER`; `Report.targetId = uid`, `targetAuthorId = uid`, no context.
- Rules: widen to `type in ['post','comment','user']` and allow the
  `contextId`-absent shape; **deploy before shipping the button.**
- A small "Report user" entry in the profile's top-bar overflow.

Because individual posts on the profile are already reportable, this is a
nice-to-have, not a blocker — fold into Phase 1 if it slows Phase 0 down.

### A.7 Acceptance checks
- Tapping an author's avatar/name in feed, post detail, a POI thread, and a
  comment opens that user's profile; tapping the rest of a post card still opens
  post detail.
- The profile shows avatar, name, @handle, bio, three stat tiles, and their posts
  (most-recent first), with report/block available on each post (not edit/delete).
- Viewing your own profile via this route shows no report/block/follow affordances.
- Back arrow returns to the previous screen; no bottom-nav selection change.
- Guests can open profiles (read) but see no moderation actions.

### A.8 Rough size
~2 new screens + 1 VM + 1 use case + 1 repo method, and ~5 call-site wirings.
Largest risk is the nested-clickable tap handling on the cards — already a solved
pattern in `PostCard` (the location badge), so low.

## Appendix B — Phase 1 implementation spec

The core follow mechanic: edges, denormalized counts, a Cloud-Function mirror,
rules, block integration, account-deletion cleanup, and the follow button + counts
on the Phase 0 profile. Verified against the code on 2026-06-23.
**Status: ✅ built + deployed 2026-06-23. Rules released and functions
(`onFollowCreated`, `onFollowDeleted`, `onUserBlocked`, plus the `onUserDeleted`
follow-graph cleanup) live on `echo-2b5ba`. App compiles. Manual QA pending.**

### B.1 Data model (recap, with counts)
```
users/{uid}/following/{targetUid}   { createdAt: <server time> }
users/{uid}/followers/{followerUid} { createdAt: <server time> }
users/{uid}                         { ..., followerCount: int, followingCount: int }
```
- The client writes **only** its own `following/{targetUid}` (the single source of
  truth for one edge).
- A Cloud Function mirrors `followers/{me}` onto the target and maintains both
  denormalized counts. Clients never write `followers/*` or the count fields.

### B.2 Cloud Functions ([functions/index.js](../../functions/index.js))
Add two triggers (v2 `onDocumentCreated` / `onDocumentDeleted`, same import style as
the existing functions). Both run as admin and bypass rules.

- **`onFollowCreated`** — `users/{uid}/following/{targetUid}` created:
  - `set users/{targetUid}/followers/{uid} = { createdAt }`
  - `increment users/{uid}.followingCount +1`
  - `increment users/{targetUid}.followerCount +1`
- **`onFollowDeleted`** — `users/{uid}/following/{targetUid}` deleted:
  - `delete users/{targetUid}/followers/{uid}`
  - `increment users/{uid}.followingCount -1` (guard: never < 0)
  - `increment users/{targetUid}.followerCount -1`
  - **Each op wrapped in try/catch** — during account deletion the counterpart doc
    may already be gone (NOT_FOUND must not fail the function).
- **`onUserBlocked`** — `users/{uid}` updated and a uid was newly added to
  `blockedUserIds`: for each newly-blocked `B`, delete **both** edges server-side
  (`users/{uid}/following/{B}` and `users/{B}/following/{uid}`). The mirrors +
  counts then self-correct via `onFollowDeleted`. This keeps `blockUser` on the
  client a single `arrayUnion` (unchanged) while making block authoritative.

**Idempotency caveat:** `FieldValue.increment` isn't idempotent under the rare
function retry — same tradeoff the existing `postCount`/`commentCount` counters
already accept. Acceptable for v1; a periodic reconciliation job is a later option.

### B.3 Firestore rules ([firestore.rules](../../firestore.rules))
Add inside `match /users/{uid}`:
```
// A user manages only their own `following` list. Self-follow barred; target must
// exist; blocked either-way bars the follow. createdAt pinned to request.time.
match /following/{targetUid} {
  allow read: if isSignedIn();
  allow create: if isNotAnonymous()
    && uid == request.auth.uid
    && targetUid != uid
    && exists(/databases/$(database)/documents/users/$(targetUid))
    && request.resource.data.keys().hasOnly(['createdAt'])
    && request.resource.data.createdAt == request.time
    && !(request.auth.uid in
         get(/databases/$(database)/documents/users/$(targetUid)).data.get('blockedUserIds', []))
    && !(targetUid in
         get(/databases/$(database)/documents/users/$(uid)).data.get('blockedUserIds', []));
  allow delete: if isNotAnonymous() && uid == request.auth.uid;
  allow update: if false;
}
// Mirror — function-maintained only; clients read.
match /followers/{followerUid} {
  allow read: if isSignedIn();
  allow write: if false;
}
```
- **Counts need no new rule:** the existing `users/{uid}` update rule whitelists key
  sets that *exclude* `followerCount`/`followingCount`, so no client can forge them;
  only the admin function writes them.
- The block check costs **2 extra document reads per follow** (billed) — fine at
  this scale.

### B.4 App data model
- `UserProfile` ([domain/model/UserProfile.kt](../../app/src/main/java/dev/echoapp/echo/domain/model/UserProfile.kt)):
  add `followerCount: Int = 0`, `followingCount: Int = 0`.
- `UserProfileEntity` ([data/entity/UserProfileEntity.kt](../../app/src/main/java/dev/echoapp/echo/data/entity/UserProfileEntity.kt)):
  add `followerCount: Long = 0`, `followingCount: Long = 0`; map to Int in
  `toDomain()`. (Defaults cover profiles created before this feature.)
- The viewed profile's counts come **free** from `observeProfileById` (Phase 0) —
  no extra reads.

### B.5 New app files
| File | Purpose |
|---|---|
| `domain/repository/FollowRepository.kt` | `followUser(uid)`, `unfollowUser(uid)` (suspend `Result<Unit>`), `observeIsFollowing(uid): Flow<Boolean>`. |
| `data/repository/FollowRepositoryImpl.kt` | `following/{target}` set with `serverTimestamp()` / delete; `observeIsFollowing` = snapshot listener on that doc's existence. `@Inject`, bound in `RepositoryModule`. |
| `domain/usecase/follow/FollowUserUseCase.kt` | thin wrapper. |
| `domain/usecase/follow/UnfollowUserUseCase.kt` | thin wrapper. |
| `domain/usecase/follow/ObserveIsFollowingUseCase.kt` | thin wrapper. |

### B.6 App files to edit
| File | Change |
|---|---|
| `di/RepositoryModule.kt` | `@Binds` `FollowRepositoryImpl` → `FollowRepository`. |
| `ui/profile/UserProfileUiState.kt` | add `isFollowing: Boolean`, `followerCount: Int`, `followingCount: Int`, `followInFlight: Boolean`. |
| `ui/profile/UserProfileViewModel.kt` | fold `observeIsFollowing(uid)` into the combine; add `follow()` / `unfollow()` (guard guest + self); surface counts from the profile. |
| `ui/profile/UserProfileScreen.kt` | Follow / Following button (shown when `!isSelf && !isGuest`); show follower/following counts (two stat tiles or a header row). |

### B.7 Account deletion ([functions/index.js](../../functions/index.js) `onUserDeleted`)
Subcollections survive parent-doc deletion (same reason posts' comments need a
cascade), so extend `onUserDeleted` to:
1. Delete every `users/{deleted}/following/{X}` → `onFollowDeleted` fixes each `X`'s
   `followerCount` and removes the mirror.
2. For every `users/{deleted}/followers/{Y}`, delete `users/{Y}/following/{deleted}`
   → `onFollowDeleted` fixes each `Y`'s `followingCount`.
3. `deleteCollection` both subcollections to mop up leftovers (idempotent).

### B.8 Indexes
**None.** `observeIsFollowing` is a single-doc get; the Phase 3 follower/following
lists order by `createdAt` within one subcollection (auto single-field index). The
distance-free *feed* index (`authorId + timestamp`) belongs to **Phase 2**, not here.

### B.9 Acceptance checks
- Tapping **Follow** on another profile makes their `followerCount` and your
  `followingCount` rise by one; **Following** → tap reverts both. Button reflects
  state live across app restarts (driven by `observeIsFollowing`).
- A follow writes exactly one doc (`users/{me}/following/{target}`); the
  `followers` mirror appears server-side shortly after.
- Self-follow is impossible (no button on your own profile; rules also reject).
- Guests see no Follow button.
- Blocking someone you follow (or who follows you) severs both edges and both
  counts drop; you can't re-follow a blocked user (rules reject).
- Deleting an account leaves no dangling edges and no stale counts on others.

### B.10 Watch-outs
- **Deploy order:** ship the **functions + rules first**, then the app — the app's
  follow write fails closed until the rules allow it, and counts stay 0 until the
  functions run.
- **increment idempotency** under retries (B.2) — accepted for v1.
- **Optimistic UI:** the button can flip immediately while the count trails by the
  function's latency; reflect `followInFlight` so double-taps don't double-write.
- **Blocked-profile view:** viewing the profile of someone you've blocked should
  hide Follow (you can't follow them anyway) — gate on the existing hidden-ids
  signal if we want it tidy.

## Appendix C — Phase 2 implementation spec (Following feed)

The payoff: a feed of posts from people you follow, **regardless of the 5km
radius**. Verified against the feed code on 2026-06-23.
**Status: ✅ built + deployed 2026-06-23. App compiles; the `authorId + timestamp`
composite index is deployed to `echo-2b5ba` (allow a few minutes for Firestore to
finish building it over existing posts). Manual QA pending.**
Depends on Phase 1 (the `following` edges) being live — it is.

### C.1 Design decision — chunked `in` query, one-shot, segmented toggle
- **Placement:** a segmented toggle at the top of the existing feed —
  **"Nearby" | "Following"** — not a new bottom-nav tab. Least disruption; the feed
  screen already owns the post list, report/block, and like plumbing.
- **Query:** read my `following` ids, chunk into groups of ≤30, run
  `posts where authorId in <chunk> orderBy timestamp desc` per chunk, then merge +
  sort + dedupe client-side. **No geohash filter** — that's the distance-free point.
- **One-shot + pull-to-refresh** (mirrors the existing *tag* view), **not** the
  Room-cached offline feed. Keeps v1 simple; the cached/offline + true pagination
  story is what the Phase 4 fan-out timeline is for. Blocked authors are still
  filtered reactively via `observeHiddenAuthorIdsUseCase`.
- **Why not live listeners:** one snapshot listener per 30-id chunk gets expensive
  for large follow counts. A one-shot read refreshed on entry/pull is the right v1.

### C.2 Firestore index ([firestore.indexes.json](../../firestore.indexes.json))
Add — **required before this ships or the query throws:**
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "authorId", "order": "ASCENDING" },
    { "fieldPath": "timestamp", "order": "DESCENDING" }
  ]
}
```
Deploy with `firebase deploy --only firestore:indexes --project echo-2b5ba`
(building the index over existing posts takes a few minutes).

### C.3 New / changed data layer
| File | Change |
|---|---|
| `domain/repository/FollowRepository.kt` | add `observeFollowingIds(): Flow<List<String>>` (snapshot on `users/{me}/following`). |
| `data/repository/FollowRepositoryImpl.kt` | implement it (empty list when signed out / guest). |
| `domain/repository/PostRepository.kt` | add `suspend fun getPostsByAuthors(authorIds: List<String>, limit: Long): List<Post>`. |
| `data/repository/PostRepositoryImpl.kt` | implement: chunk `authorIds` into ≤30, `whereIn("authorId", chunk).orderBy(timestamp, DESC).limit(limit)` per chunk via `get()`, flatten, `distinctBy { id }`, sort desc, take `limit`. Empty `authorIds` → `emptyList()` (skip the query). |
| `domain/usecase/post/GetFollowingFeedUseCase.kt` | new: `operator fun invoke(): Flow<List<Post>>` = `observeFollowingIds()` → `flatMapLatest`/`mapLatest` → `getPostsByAuthors(ids, POSTS_QUERY_LIMIT)`. Emits empty when not following anyone. |

### C.4 Presentation
| File | Change |
|---|---|
| `ui/feed/FeedUiState.kt` | add `feedMode: FeedMode` (`NEARBY` / `FOLLOWING`); maybe `isFollowingEmpty`. |
| `ui/feed/FeedViewModel.kt` | a `_feedMode` `MutableStateFlow`; `flatMapLatest` over it: `NEARBY` → existing cached/paginated feed, `FOLLOWING` → `getFollowingFeedUseCase()` (blocked-filtered, refreshable). `setFeedMode(mode)`. Pagination/`loadMore` only applies in `NEARBY` (already guards on tag; extend to mode). |
| `ui/feed/FeedScreen.kt` | a `SingleChoiceSegmentedButtonRow` ("Nearby"/"Following") under the top bar; tab filter only in Nearby. Following empty state: *"Follow people to see their echoes anywhere."* Guests: prompt to sign in (they can't follow). Reuse the existing `PostCard` wiring incl. `onAuthorClick`. |

### C.5 Watch-outs
- **`in` cap is 30** ids/query — chunk strictly. Very large follow counts mean many
  chunk reads per refresh; acceptable for v1, and the reason Phase 4 exists.
- **No infinite scroll** in Following v1 (merged-chunk cursors are fiddly) — show the
  newest `POSTS_QUERY_LIMIT`. Note in the empty/end affordance if needed.
- **Reads cost:** each refresh reads up to `limit` posts per chunk. Fine at launch
  scale; revisit with fan-out if it grows.
- **Reactivity:** new posts from followees appear on refresh / re-entry, not live
  (the deliberate one-shot tradeoff).
- **Deploy order:** ship the **index first**, then the app.

## Appendix D — Phase 3 implementation spec (polish)

Three independent pieces; ship in any order. Verified against the code on
2026-06-23. **Status: ✅ built 2026-06-23 (app compiles; no backend/deploy needed).
Manual QA pending.** D.1 own-profile counts, D.2 follower/following list screens
(`FollowListScreen` at route `follow_list/{uid}/{type}`, counts tappable on both
profiles), and D.3 Data Safety doc all implemented.

### D.1 Follow counts on your own profile (smallest; closes the QA gap)
The own-profile screen still shows only Posts/Likes/Comments — the follower/
following counts only appear on *other* people's profiles. The data is already there:
`ProfileUiState.userProfile` is a `UserProfile`, which now carries
`followerCount`/`followingCount` (Phase 1).
- **Edit only [ui/profile/ProfileScreen.kt](../../app/src/main/java/dev/echoapp/echo/ui/profile/ProfileScreen.kt):**
  add a Followers/Following row under the `@handle` (reuse the `FollowStat`
  composable already written for `UserProfileScreen`, or lift it to a shared file).
- No ViewModel/repo change. ~10 lines.

### D.2 Follower / Following list screens
Tap a follower/following count (on either your own or another profile) → a list of
those users, each row tappable through to their profile.
- **Repository ([FollowRepository] + impl):** `suspend fun getFollowerIds(uid)` and
  `getFollowingIds(uid)` — one-shot reads of the `followers`/`following`
  subcollections ordered by `createdAt` desc, capped (e.g. 200). (The existing
  `observeFollowingIds()` is current-user + live; these are arbitrary-uid + one-shot.)
- **Use case** `GetFollowListUseCase(uid, type)` → `List<UserProfile>`: read the ids,
  then `userRepository.getProfilesByIds(ids)` (already exists; preserves order).
- **Screen** `FollowListScreen` + `FollowListViewModel` + `FollowListUiState`; route
  `follow_list/{uid}/{type}` (`type` = `followers`|`following`). A two-tab screen
  (Followers | Following) opened on the tapped tab is the nicer UX; a row = avatar +
  name + @handle → `user_profile/{uid}`.
- **Wire the counts** in `UserProfileScreen` (and the D.1 own-profile row) to navigate
  to `follow_list/{uid}/<type>`.

**Decisions for v1 (recommended):**
- **No inline follow button** on list rows — keep rows tap-to-profile. (Inline follow
  needs per-row `isFollowing` state; defer.)
- **Load-all-capped, no pagination** — fine at launch scale; `getProfilesByIds` is N
  gets, so the 200 cap bounds cost. Revisit if lists grow.
- Empty states: "No followers yet" / "Not following anyone yet".

### D.3 Data Safety form update (launch requirement)
Echo now stores a **social graph** (who follows whom). Update
[DATA_SAFETY.md](DATA_SAFETY.md): it's still **Collected, not Shared** — add a row
under **App activity** (e.g. "Other user-generated content" / social connections) so
the Play declaration stays accurate before submission. No code.

### D.4 Watch-outs
- `getProfilesByIds` does one read per id — the 200 cap matters; a deleted user's
  leftover edge resolves to a null profile and is skipped (already handled by the
  mapper returning null for blank uid).
- A follow list is public (profiles are world-readable to signed-in users) — matches
  the one-way-public-follow decision; no privacy regression.

## Open questions / watch-outs

- **Legacy posts with empty `authorId`** (`Post` notes some legacy docs predate
  the field) won't appear in a `authorId in [...]` feed. Acceptable — they're
  old and rare.
- **Self-follow** must be prevented (rules + client).
- **Composite index** must be deployed *before* the Following feed ships or the
  query throws.
- **Re-follow after unfollow** is allowed; only a *block* bars it.
