package org.joinmastodon.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;

public class GlobalUserPreferences{
	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean altTextReminders, confirmUnfollow, confirmBoost, confirmDeletePost;
	public static ThemePreference theme;
	public static LoadMissingPostsPreference loadMissingPosts;

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	public static void load(){
		SharedPreferences prefs=getPrefs();
		playGifs=prefs.getBoolean("playGifs", true);
		useCustomTabs=prefs.getBoolean("useCustomTabs", true);
		altTextReminders=prefs.getBoolean("altTextReminders", false);
		confirmUnfollow=prefs.getBoolean("confirmUnfollow", false);
		confirmBoost=prefs.getBoolean("confirmBoost", false);
		confirmDeletePost=prefs.getBoolean("confirmDeletePost", true);
		theme=ThemePreference.values()[prefs.getInt("theme", 0)];
		loadMissingPosts=LoadMissingPostsPreference.values()[prefs.getInt("loadMissingItems", 0)];
	}

	public static void save(){
		getPrefs().edit()
				.putBoolean("playGifs", playGifs)
				.putBoolean("useCustomTabs", useCustomTabs)
				.putInt("theme", theme.ordinal())
				.putBoolean("altTextReminders", altTextReminders)
				.putBoolean("confirmUnfollow", confirmUnfollow)
				.putBoolean("confirmBoost", confirmBoost)
				.putBoolean("confirmDeletePost", confirmDeletePost)
				.putInt("loadMissingItems", loadMissingPosts.ordinal())
				.apply();
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}

	public enum LoadMissingPostsPreference{
		NEWEST_FIRST(R.string.load_missing_posts_newest_first), // Downwards, default
		OLDEST_FIRST(R.string.load_missing_posts_oldest_first); // Upwards

		@StringRes
		public int labelRes;

		LoadMissingPostsPreference(@StringRes int labelRes){
			this.labelRes=labelRes;
		}
	}
}
