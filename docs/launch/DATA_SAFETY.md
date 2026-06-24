# Echo — Play Console Data Safety answers

Transcribe these into Play Console → **App content → Data safety**. Kept here so
the declaration stays consistent with [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

> **Key definitions (Google's, easy to get wrong):**
> - **Collected** = sent off the device. Echo sends data to Firebase, so yes.
> - **Shared** = transferred to a *third party / separate company*. Using Firebase
>   (a service provider/processor) is **NOT** "sharing", and other Echo users
>   seeing your posts is **NOT** "sharing" either. So Echo shares **nothing**.

## Overview questions
- **Does your app collect or share any of the required user data types?** → **Yes**
- **Is all of the user data collected by your app encrypted in transit?** → **Yes** (HTTPS/TLS via Firebase)
- **Do you provide a way for users to request that their data be deleted?** → **Yes**
  (in-app: Settings → Account → Delete account; web: the hosted deletion page)

For every data type below: **Shared = No**, **Collected = Yes**, and unless noted,
purpose = **App functionality** and **Account management**.

| Category | Data type | Collected | Optional/Required | Purpose |
|---|---|---|---|---|
| Location | **Precise location** | Yes | **Optional** (only if location permission granted) | App functionality |
| Personal info | **Email address** | Yes | Required (registered accounts) | Account management |
| Personal info | **Name** (first/last) | Yes | Required (registered accounts) | Account management |
| Personal info | **User IDs** (@username, Firebase UID) | Yes | Required (registered accounts) | Account management, App functionality |
| App activity | **Other user-generated content** (posts, comments, bio, tags, likes) | Yes | Optional | App functionality |
| App activity | **Other actions** (follow relationships / social connections) | Yes | Optional | App functionality |
| App info & performance | **Crash logs** | Yes | — | Crash diagnostics (App functionality / Analytics) |
| App info & performance | **Diagnostics** (Crashlytics performance data) | Yes | — | App functionality |

## Notes / judgment calls
- **Precise vs approximate location:** Echo uses HIGH_ACCURACY GPS, so declare
  **Precise location**. It's **Optional** — the app works without granting it.
- **User-generated content is "Collected, not Shared."** Posts being visible to
  other users is *not* "sharing" under Google's definition (that's about third
  parties), so leave Shared = No.
- **Follow graph (social connections):** Echo stores who follows whom
  (`users/{uid}/following` + `followers`). Declare it under **App activity** — Play's
  taxonomy has no dedicated "social graph" type, so map it to **"Other actions"** (or
  fold into "Other user-generated content" if you'd rather not add a row; either is
  defensible). Still **Collected, not Shared** — other users seeing your follower
  count isn't third-party sharing. Optional (only registered, non-guest users follow).
- **App Check / Play Integrity:** this attests device/app integrity; it does not
  collect a persistent advertising/device ID for you. Generally **not** declared
  as "Device or other IDs." Only declare that category if you later add analytics
  that read a device identifier.
- **No photos:** image upload is out of scope for v1, so do **not** declare Photos.
- **Guest mode** collects none of the above (anonymous, no profile) — declarations
  cover registered accounts.
