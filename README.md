# Mastodon for Android

[![Crowdin](https://badges.crowdin.net/mastodon-for-android/localized.svg)](https://crowdin.com/project/mastodon-for-android)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.joinmastodon.android/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.joinmastodon.android)

You can also get the APK from the [the Releases section](https://github.com/mastodon/mastodon-android/releases/latest).

## Introduction

This is the repository for the official Android app for Mastodon.

Please note that this app is intended to be used with Mastodon servers. Our team does not have bandwidth to ensure compatibility with other server software, which means that unsolicited pull requests focused on that goal will most likely be closed.

## Contributing

First, please read the Mastodon project [Contributing guide](https://github.com/mastodon/.github/blob/main/CONTRIBUTING.md).

Note that user interface changes for our official apps are carried out through a design process that involves core team review - most changes of this kind will not be accepted as community contributions; if they are accepted, they will take time to go through this review.

If you would like to help translate the app into your language, please go to [Crowdin](https://crowdin.com/project/mastodon-for-android). If your language is not listed in the Crowdin project, please create an issue and we will add it. Please do not create pull requests that modify `strings.xml` files for languages other than English.

## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```shell
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

The Mastodon name and logo are trademarks. If you intend to redistribute a modified version of this app, use a unique name and icon for your app that does not mistakenly imply any official connection with or endorsement by the Mastodon non-profit organisation.
