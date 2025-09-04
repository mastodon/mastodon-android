package org.joinmastodon.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

public class GlobalUserPreferences{
	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean altTextReminders, confirmUnfollow, confirmBoost, confirmDeletePost;
	public static ThemePreference theme=ThemePreference.AUTO;
	public static boolean useDynamicColors;
	public static boolean showInteractionCounts;
	public static boolean customEmojiInNames;
	public static boolean showCWs;
	public static boolean hideSensitiveMedia;

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	private static SharedPreferences getPreReplyPrefs(){
		return MastodonApp.context.getSharedPreferences("pre_reply_sheets", Context.MODE_PRIVATE);
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
		useDynamicColors=prefs.getBoolean("useDynamicColors", true);
		showInteractionCounts=prefs.getBoolean("interactionCounts", true);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		showCWs=prefs.getBoolean("showCWs", true);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
		if(!prefs.getBoolean("perAccountMigrationDone", false)){
			AccountSession account=AccountSessionManager.getInstance().getLastActiveAccount();
			if(account!=null){
				SharedPreferences accPrefs=account.getRawLocalPreferences();
				showInteractionCounts=accPrefs.getBoolean("interactionCounts", true);
				customEmojiInNames=accPrefs.getBoolean("emojiInNames", true);
				showCWs=accPrefs.getBoolean("showCWs", true);
				hideSensitiveMedia=accPrefs.getBoolean("hideSensitive", true);
				save();
			}
			// Also applies to new app installs
			prefs.edit().putBoolean("perAccountMigrationDone", true).apply();
		}
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
				.putBoolean("useDynamicColors", useDynamicColors)
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("showCWs", showCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.apply();
	}

	public static boolean isOptedOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		if(getPreReplyPrefs().getBoolean("opt_out_"+type, false))
			return true;
		if(account==null)
			return false;
		String accountKey=account.acct;
		if(!accountKey.contains("@"))
			accountKey+="@"+AccountSessionManager.get(accountID).domain;
		return getPreReplyPrefs().getBoolean("opt_out_"+type+"_"+accountKey.toLowerCase(), false);
	}

	public static void optOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		String key;
		if(account==null){
			key="opt_out_"+type;
		}else{
			String accountKey=account.acct;
			if(!accountKey.contains("@"))
				accountKey+="@"+AccountSessionManager.get(accountID).domain;
			key="opt_out_"+type+"_"+accountKey.toLowerCase();
		}
		getPreReplyPrefs().edit().putBoolean(key, true).apply();
	}

	public static void resetPreReplySheets(){
		getPreReplyPrefs().edit().clear().apply();
	}

	public static boolean alertSeen(String key){
		return getPreReplyPrefs().getBoolean("alertSeen_"+key, false);
	}

	public static void setAlertSeen(String key){
		getPreReplyPrefs().edit().putBoolean("alertSeen_"+key, true).apply();
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}

	public enum PreReplySheetType{
		OLD_POST,
		NON_MUTUAL
	}
}
