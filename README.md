Mastodon for Android
======================

[![Crowdin](https://badges.crowdin.net/mastodon-for-android/localized.svg)](https://crowdin.com/project/mastodon-for-android)

This is the repository for the official Android app for Mastodon.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.joinmastodon.android/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.joinmastodon.android)

Or get the APK from the [The Releases Section](https://github.com/mastodon/mastodon-android/releases/latest).

## Contributing

Our goal is delivering a polished, professionally designed and user-friendly app. We proceed according to wireframes provided by a professional UX designer that works with Mastodon gGmbH. This means that any outside contributions that change the app visually must first be coordinated with the UX designer. *This can take time.* Furthermore, we work off of an internal roadmap and aim for feature-parity and consistency with our iOS app. The iOS app is designated as the "primary" between the two, therefore, if you want to request features, please do so in the [Mastodon for iOS](https://github.com/mastodon/mastodon-ios) repository, as you are requesting a feature to be both in iOS and Android (exceptions being system integrations specific to Android). On the other hand, any contributions that improve existing functionality, performance, or accessibility should not have any roadblocks to being merged.

If you would like to help translate the app into your language, please go to [Crowdin](https://crowdin.com/project/mastodon-for-android). If your language is not listed in the Crowdin project, please create an issue and we will add it. Please do not create pull requests that modify `strings.xml` files for languages other than English.

## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

The Mastodon name and logo are trademarks of Mastodon gGmbH. If you intend to redistribute a modified version of this app, use a unique name and icon for your app that does not mistakenly imply any official connection with or endorsement by Mastodon gGmbH.
