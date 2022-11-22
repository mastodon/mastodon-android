Mastodon for Android
======================

[![Crowdin](https://badges.crowdin.net/mastodon-for-android/localized.svg)](https://crowdin.com/project/mastodon-for-android)

<a href="https://play.google.com/store/apps/details?id=org.joinmastodon.android"><img src="img/google-play-badge.png" height="50"></a>

This is the repository for the official Android app for Mastodon.

## Contributing

Our goal is delivering a polished, professionally designed and user-friendly app. We proceed according to wireframes provided by a professional UX designer that works with Mastodon gGmbH. This means that any outside contributions that change the app visually must first be coordinated with the UX designer. *This can take time.* Furthermore, we work off of an internal roadmap and aim for feature-parity and consistency with our iOS app. The iOS app is designated as the "primary" between the two, therefore, if you want to request features, please do so in the [Mastodon for iOS](https://github.com/mastodon/mastodon-ios) repository, as you are requesting a feature to be both in iOS and Android (exceptions being system integrations specific to Android). On the other hand, any contributions that improve existing functionality, performance, or accessibility should not have any roadblocks to being merged.

## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

The Mastodon name and logo are trademarks of Mastodon gGmbH. If you intend to redistribute a modified version of this app, use a unique name and icon for your app that does not mistakenly imply any official connection with or endorsement by Mastodon gGmbH.
