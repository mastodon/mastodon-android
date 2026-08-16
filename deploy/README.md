# Server-side files for masto.nyc

## `assetlinks.json` — Android App Links

Android will only let this app claim `https://masto.nyc/...` links if masto.nyc vouches for it.
Until this file is live, `https://masto.nyc/.well-known/assetlinks.json` returns **404**, domain
verification fails, and the `autoVerify="true"` profile-link filter in `AndroidManifest.xml` does
nothing — tapping a `masto.nyc/@user` link stays in the browser.

### Deploying

Drop it into the Mastodon install's `public/` directory, which is served as-is:

    public/.well-known/assetlinks.json

Then check it from outside:

    curl -i https://masto.nyc/.well-known/assetlinks.json

It must return **200**, `Content-Type: application/json`, and **no redirect**. Android follows
neither redirects nor HTTP here. If nginx has a `location /.well-known/` block for ACME/certbot
that shortcuts everything under that path, it will shadow this file — check there first if the
curl comes back 404 after deploying.

### Re-verifying on a device

Verification is attempted at install time, so an already-installed app will not pick it up:

    adb shell pm verify-app-links --re-verify nyc.masto.android
    adb shell pm get-app-links nyc.masto.android

You are looking for `masto.nyc: verified`.

### Fingerprints

The fingerprint currently listed is a **debug** keystore — the one on the machine that built the
APK. It is fine for testing and useless to anyone else, since every developer's debug keystore
differs.

Before release, add the release signing key's fingerprint. The field is an array, so keep both and
debug builds continue to work:

    keytool -list -v -keystore <release>.jks -alias <alias> | grep SHA256

If the app is ever distributed through Google Play with Play App Signing, the fingerprint that
matters is the one Play shows under *Release → Setup → App signing*, not your local upload key.

## What this does not fix

Publishing this file does **not** by itself make the signup confirmation email return to the app.
That link redirects to the OAuth app's registered `redirect_uri`, which is the custom scheme
`nyc.masto.android-auth://callback`, and browsers commonly block server-initiated redirects to
custom schemes. Fixing that properly means changing `AccountSessionManager.REDIRECT_URI` to an
`https://masto.nyc/...` App Link and registering it in the manifest — which only works once this
file is live and verified, and which breaks login entirely if verification ever fails. Worth doing
deliberately, not as a side effect.

In the meantime `AccountActivationFragment` polls `GetOwnAccount` every 10 seconds and re-checks
whenever it becomes visible, so switching back to the app after confirming in the browser advances
onboarding on its own.
