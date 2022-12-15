# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all model classes as they're used with gson and their names are shown in errors
-keep public class org.joinmastodon.android.model.**{
	<fields>;
}

# Inner classes in api requests are used with gson
-keepclassmembers class org.joinmastodon.android.api.**$*{
	*;
}

# Keep all enums for debugging purposes
-keepnames public enum * {
	*;
}

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
  @com.squareup.otto.Subscribe <methods>;
}

-keep class com.microsoft.appcenter.** {
	*;
}

-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }

-keep class org.joinmastodon.android.AppCenterWrapper { *; }

-keepattributes LineNumberTable