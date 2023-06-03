package org.joinmastodon.android.api.session;

import android.content.SharedPreferences;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean showInteractionCounts;
	public boolean customEmojiInNames;
	public boolean showCWs;
	public boolean hideSensitiveMedia;

	public AccountLocalPreferences(SharedPreferences prefs){
		this.prefs=prefs;
		showInteractionCounts=prefs.getBoolean("interactionCounts", true);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		showCWs=prefs.getBoolean("showCWs", true);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
	}

	public long getNotificationsPauseEndTime(){
		return prefs.getLong("notificationsPauseTime", 0L);
	}

	public void setNotificationsPauseEndTime(long time){
		prefs.edit().putLong("notificationsPauseTime", time).apply();
	}

	public void save(){
		prefs.edit()
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("showCWs", showCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.apply();
	}
}
