# Mastodon for Android

<a href="https://play.google.com/store/apps/details?id=org.joinmastodon.android"><img src="img/google-play-badge.png" height="50"></a>

This is the repository for the official Android app for Mastodon.

Learn more about this app in the [blog post](https://blog.joinmastodon.org/2022/02/official-mastodon-for-android-app-is-coming-soon/).

## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).