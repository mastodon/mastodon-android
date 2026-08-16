# Fork notes

This repo is a fork of [mastodon/mastodon-android](https://github.com/mastodon/mastodon-android),
rebranded as **Masto NYC** and locked to the **masto.nyc** server.

The whole point of the structure below is that pulling upstream stays a routine chore, not a
project. Every rule here exists to keep the number of upstream lines we touch as small as possible.

## Conventions

**1. Values live in `ForkConfig`, not scattered through upstream files.**

[`ForkConfig.java`](mastodon/src/main/java/org/joinmastodon/android/fork/ForkConfig.java) holds
the server domain and anything else that differs from upstream. It is a new file in a new package,
so it can never conflict. When you need a fork-specific value inside an upstream file, add an import
and read it from `ForkConfig` — that keeps the upstream edit down to a line or two.

**2. New strings go in a new file.**

Genuinely new strings belong in `mastodon/src/main/res/values/strings_nyc.xml` (create it when the
first one is needed), never appended to upstream's `strings.xml`. Only *overrides* of an existing
upstream string may be edited in place in `values/strings.xml`, because Android forbids the same
resource name in two files within a source set.

**3. Don't delete upstream files.**

`InstanceChooserLoginFragment`, `InstanceCatalogSignupFragment`, and `InstanceCatalogFragment` are
now unreachable — nothing navigates to them. They are deliberately left in the tree anyway. Deleting
a file that upstream still maintains turns every future change to it into a modify/delete conflict,
which is far more annoying to resolve than carrying dead code. The same goes for the
`intro_bottom_sheet` layout and the `welcome_*` / `pick_server` / `learn_more` strings.

**4. Mark non-obvious edits.**

Where a change to an upstream file isn't self-explanatory, prefix the comment with
`// masto.nyc fork:` so it's obvious during a merge that the line is intentionally ours.

## Merging upstream

```bash
git remote add upstream https://github.com/mastodon/mastodon-android
git fetch upstream
git merge upstream/master
```

Conflicts, if any, will be confined to the files listed below.

## Upstream files this fork touches

| File | Change |
| --- | --- |
| `mastodon/build.gradle` | `applicationId` → `nyc.masto.android`; added `compileSdkMinor 0` |
| `mastodon/src/main/AndroidManifest.xml` | deep links point at `masto.nyc` instead of `mastodon.social` / `mastodon.online` |
| `mastodon/src/main/res/values/strings.xml` | `app_name`, `settings_contribute`, `settings_app_version` |
| `mastodon/src/main/res/values/urls.xml` | `github_url`, `privacy_policy_url` |
| `mastodon/src/main/res/layout/fragment_splash.xml` | dropped "Pick another server", "Learn more", and the now-unused progress overlay |
| `.../fragments/SplashFragment.java` | server is fixed; log in goes straight to OAuth; no server catalog request |
| `.../fragments/onboarding/GoogleMadeMeAddThisFragment.java` | privacy policy item points at our policy |
| `.../api/requests/oauth/CreateOAuthApp.java` | OAuth client name and website |
| `.../api/MastodonAPIController.java`, `.../MastodonApp.java` | `User-Agent`; see [below](#user-agent) |
| `.../updater/GithubSelfUpdaterImpl.java` | githubRelease builds self-update from this repo, not upstream's |
| `res/drawable/splash_logo.xml`, `res/drawable/ic_ntf_logo.xml` | replaced artwork; see [Artwork](#artwork) |
| `res/drawable-anydpi-v26/ic_launcher_{foreground,background,monochrome}.xml` | replaced artwork |
| `res/mipmap-*/ic_launcher.png` | replaced artwork |
| `README.md`, `fastlane/metadata/android/en-US/*` | store listing and repo docs |

New files, which can never conflict: `ForkConfig.java`, `FORK.md`, `deploy/`, and the generated
artwork under `res/drawable-*dpi/` (`ic_launcher_elephant{,_mono}.png`, `splash_logo_5bfp.png`).

Changing `applicationId` automatically carries the OAuth callback scheme
(`${applicationId}-auth://callback`) and the FileProvider authority along with it — neither needs a
separate edit. Verified in the built APK: the manifest ends up with `nyc.masto.android-auth`.

### Why `compileSdkMinor 0` is there

This one is not a branding change, it is a build fix, and upstream would hit it too on a fresh SDK.
Google no longer publishes a bare `platforms;android-37` — every API 37 platform is minor-versioned
(`android-37.0`, `android-37.1`, plus `37.2` betas), and `source.properties` reports
`AndroidVersion.ApiLevel=37.0`. Upstream's bare `compileSdk 37` therefore fails with:

    Failed to find target with hash string 'android-37' in: <sdk>

AGP 8.13.2 does support `compileSdkMinor` even though upstream does not use it, so adding
`compileSdkMinor 0` next to `compileSdk 37` resolves the platform to `android-37.0` and the build
succeeds. If a future upstream merge adopts a different fix for the same problem, drop this line.

### User-Agent

Upstream sends `MastodonAndroid/<versionName>`, which would make this fork indistinguishable from
the official app in any instance's logs. It now sends `MastoNYCAndroid/<versionName>`, from
`ForkConfig.USER_AGENT_PRODUCT`.

There are **two** places that set it, and it is easy to fix only one — a case-sensitive grep for
`userAgent` misses `NetworkUtils.setUserAgent`:

| Where | Covers |
| --- | --- |
| `MastodonAPIController.submitRequest` | every Mastodon API call |
| `MastodonApp.onCreate` → `NetworkUtils.setUserAgent` | appkit's image/media fetching |

The `Android` suffix is deliberate: it keeps the platform visible to server admins and anything
already matching `*Android/*` keeps working. No device model or OS version is included — upstream
sends none, and adding them would hand every instance a fingerprinting signal for no user benefit.

Worth re-checking after an upstream merge that introduces new HTTP clients. To audit a built APK:

```bash
for d in $(unzip -l app.apk | grep -oE "classes[0-9]*\.dex"); do
  unzip -p app.apk $d | strings | grep -c MastodonAndroid
done
```

## Toolchain

- **JDK 21** (Temurin), matching what both CI workflows pin. Do not go newer: the Gradle wrapper
  pulls Gradle 8.13, which predates JDK 24/25 support.
- **Android SDK Platform 37.0** — note the `.0`; see above.
- Build-Tools are downloaded by AGP itself once SDK licences are accepted; no need to pin a version.
- `local.properties` (gitignored) needs `sdk.dir=<path to SDK>`.

Verified: `./gradlew assembleDebug` produces a 7.2 MB APK with package `nyc.masto.android` and
label `Masto NYC`, which installs and runs on a physical device — splash, signup, email activation
polling and branding all confirmed on-device, not just compiled.

## Artwork

Upstream's license notice requires a redistributed fork to use a distinct name **and icon**, so the
trademarked marks had to go. The identity artwork is now Five Borough Fedi Project's; the
background illustrations are still upstream's and are tracked below.

Replacements keep the **same filename and same path** — nothing in code or layout needs to change,
and it keeps the merge surface at zero.

The launcher icon source art is `5bfplogo.png` (yellow elephant, green Lady Liberty crown); the
generated layers live in `drawable-{m,h,x,xx,xxx}hdpi/ic_launcher_elephant{,_mono}.png` on a 108dp
canvas with the art at 62% so no mask clips the trunk or crown.

The splash logo is the 5BFP lockup, keyed off its white JPEG background. Two things had to be
separated by hand there, and they're worth knowing if the art is ever re-exported:

- The elephant's cream tusk and eye highlight must stay opaque; the counters inside the **B** and
  **P** must knock through to the background or they read as printing errors on the blue. The
  background was found by flooding inward from the border, then the counters were isolated by
  eroding the enclosed white regions to seeds (which erases the small eye highlight) and flooding
  those seeds back out.
- The lockup is **2.32:1**, not the 3.86:1 of upstream's wordmark, so `fragment_splash.xml`'s
  ImageView went from 300×78dp to 300×129dp. Any re-export at a different ratio needs that height
  updated to match, or `fitCenter` will letterbox it.

`ic_ntf_logo` is the **Liberty crown on its own**, and it stayed a **vector** while the other
replacements became bitmaps. Both decisions have reasons worth remembering:

- The full elephant is illegible at 24dp — it reads as a blob, and knocking out the eye to fix that
  produces something distinctly unfriendly. The crown alone survives the size and is unambiguously
  NYC. It is one solid contour with no knockouts, so it needs no `android:fillType` — which also
  sidesteps the fact that minSdk 23 predates that attribute (API 24) and silently falls back to
  nonZero winding.
- Despite the name it is not only the notification small icon. `ProfileQrCodeFragment` drops it into
  the middle of the profile QR code, and `FancyQrCodeDrawable` draws it at `size/3` of a QR that
  `saveCodeAsFile` renders at 1080×1080 — roughly 360px. A 24dp bitmap would visibly blur there.
  `LinkCardHolder` uses it too, at 17dp.

### Identity marks — replaced

| File | Format | Size | Used for |
| --- | --- | --- | --- |
| ~~`drawable-anydpi-v26/ic_launcher_foreground.xml`~~ | bitmap wrapper | — | **Done** — wraps `@drawable/ic_launcher_elephant` |
| ~~`drawable-anydpi-v26/ic_launcher_background.xml`~~ | shape | — | **Done** — flat `#FFFFFF`; change this one line to retheme the icon |
| ~~`drawable-anydpi-v26/ic_launcher_monochrome.xml`~~ | bitmap wrapper | — | **Done** — wraps `@drawable/ic_launcher_elephant_mono` |
| ~~`mastodon/src/main/res/mipmap-{m,h,x,xx,xxx}hdpi/ic_launcher.png`~~ | PNG ×5 | 48 / 72 / 96 / 144 / 192 px | **Done** — Five Borough elephant. Launcher icon, API 23–25 only |
| ~~`mastodon/src/main/res/drawable/splash_logo.xml`~~ | bitmap wrapper | — | **Done** — 5BFP lockup; wraps `@drawable/splash_logo_5bfp` |
| ~~`mastodon/src/main/res/drawable/ic_ntf_logo.xml`~~ | vector | 24×24dp, viewport 24×24 | **Done** — the Liberty crown alone, traced from the 5BFP mark |
| ~~`fastlane/metadata/android/en-US/images/icon.png`~~ | PNG | 512×512 | **Done** — same elephant as the launcher icon, on white, full-bleed and fully opaque |
| ~~`fastlane/metadata/android/en-US/images/featureGraphic.png`~~ | PNG | 1024×500 | **Done** — subway scene, cropped from `Mastodon_Image_signed2.png` |

Note that neither store image is currently uploaded by CI: both Fastfile lanes pass
`skip_upload_images: true`, and both workflows set `SUPPLY_SKIP_UPLOAD_METADATA: true`. Until that
changes, these files are the source of truth in git but the live listing has to be set in the Play
Console by hand.

### Still upstream's — Mastodon mascot illustrations

These are background illustrations rather than identity marks, so they are not license-blocking,
but they are recognisably Mastodon's mascot and are the most visible remaining giveaway — the three
elephants on the splash screen are these.

The splash art is five parallax layers; the layout scales them to a 360×640dp stage, so keep each
one's aspect ratio or the parallax offsets in `fragment_splash.xml` will need retuning.

| File | Size (px) | Drawn at | Layer |
| --- | --- | --- | --- |
| `mastodon/src/main/res/drawable-nodpi/splash_art_layer0.webp` | 870×1137 | 414×541dp | Clouds |
| `mastodon/src/main/res/drawable-nodpi/splash_art_layer4.webp` | 656×195 | 245.64×72.65dp | Elephant on a paper plane |
| `mastodon/src/main/res/drawable-nodpi/splash_art_layer1.webp` | 443×518 | 150.84×176.44dp | Right hill |
| `mastodon/src/main/res/drawable-nodpi/splash_art_layer2.webp` | 599×466 | 197.2×153.61dp | Left hill |
| `mastodon/src/main/res/drawable-nodpi/splash_art_layer3.webp` | 870×756 | 400×346dp | Centre hill |
| `mastodon/src/main/res/drawable/empty_state_elephant_light.xml` | viewport 400×400 | 200×200dp | Empty-list state, light theme |
| `mastodon/src/main/res/drawable/empty_state_elephant_dark.xml` | viewport 400×400 | 200×200dp | Empty-list state, dark theme |

Splash background fills are hardcoded in `fragment_splash.xml` as `#50D5ED` (top) and `#478E6A`
(bottom); change those alongside the art if the new palette doesn't match.

### Optional

- `fastlane/metadata/android/en-US/images/phoneScreenshots/1–8.png` — store screenshots, still show
  upstream branding and the old splash.
- `mastodon/src/main/res/drawable-nodpi/donation_successful_art.webp` — already unreachable, since
  upstream only offers donations to `mastodon.social` / `mastodon.online` accounts.

Not branded, no action needed: `ic_shortcut_compose` / `ic_shortcut_explore` (generic glyphs),
`ic_notification_fallback` (a black dot), `poof.png`.

## Server-side pieces

`deploy/` holds files that belong on the masto.nyc server rather than in the app.
`assetlinks.json` is the one that matters: without it Android cannot verify this app's claim on
`https://masto.nyc/...`, so the `autoVerify="true"` profile-link filter in the manifest silently
does nothing. See `deploy/README.md`.

## Deliberately not changed: "Open email app"

On the signup confirmation screen this button uses
`Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_EMAIL)`. That is the documented Android
intent for the job and there is no better platform API — Android has no "open the inbox" contract
beyond it, and `mailto:` is standardised for *composing*, not for opening a mailbox.

It was briefly replaced here with a `mailto:`-resolution heuristic, then reverted. On a test device
it opened Tasker, but that needed three unusual conditions at once: no Gmail installed (Gmail does
declare the category), a mail app that declares only `mailto:`, and Tasker declaring
`CATEGORY_APP_EMAIL`. Carrying a heuristic in an upstream file to paper over that is a bad trade
against merge cost, and the heuristic has its own failure mode — Android 11+ package visibility
hides any mail app that does not also declare `http`/`https` filters, so it needs extra `<queries>`
entries and *still* surfaces false positives like a pharmacy app that registers `mailto:`.

If a user hits this, the fix is on their device: set a default mail app, or ask the mail app's
vendor to declare `CATEGORY_APP_EMAIL`.

## Known gaps

- **Non-English branding.** `app_name` is `translatable="false"` and lives only in `values/`, so the
  app name rebrands in every locale. But a handful of translated strings in `values-*/strings.xml`
  still say "Mastodon" in prose (e.g. `settings_contribute`). Overriding those would mean editing 60+
  Crowdin-managed locale files, which is exactly the merge pain this fork is structured to avoid.
- **Donation prompts** are already inert: upstream only shows them for `mastodon.social` and
  `mastodon.online` accounts, so no change was needed.
