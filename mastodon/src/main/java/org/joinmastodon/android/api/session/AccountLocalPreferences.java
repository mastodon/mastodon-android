package org.joinmastodon.android.api.session;

import android.content.SharedPreferences;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean serverSideFiltersSupported;

	public AccountLocalPreferences(SharedPreferences prefs){
		this.prefs=prefs;
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);
	}

	public long getNotificationsPauseEndTime(){
		return prefs.getLong("notificationsPauseTime", 0L);
	}

	public void setNotificationsPauseEndTime(long time){
		prefs.edit().putLong("notificationsPauseTime", time).apply();
	}

	public void save(){
		prefs.edit()
				.putBoolean("serverSideFilters", serverSideFiltersSupported)
				.apply();
	}
}
