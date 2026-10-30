package org.joinmastodon.android.api.session;

import android.content.SharedPreferences;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean serverSideFiltersSupported;
	public boolean adminReportsNotifications, adminSignupsNotifications;

	public boolean homeShowQuotes, homeShowReplies, homeShowBoosts;

	public AccountLocalPreferences(SharedPreferences prefs){
		this.prefs=prefs;
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);
		adminReportsNotifications=prefs.getBoolean("adminReports", true);
		adminSignupsNotifications=prefs.getBoolean("adminSignups", true);
		homeShowQuotes=prefs.getBoolean("homeShowQuotes", true);
		homeShowReplies=prefs.getBoolean("homeShowReplies", true);
		homeShowBoosts=prefs.getBoolean("homeShowBoosts", true);
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
				.putBoolean("adminReports", adminReportsNotifications)
				.putBoolean("adminSignups", adminSignupsNotifications)
				.putBoolean("homeShowQuotes", homeShowQuotes)
				.putBoolean("homeShowReplies", homeShowReplies)
				.putBoolean("homeShowBoosts", homeShowBoosts)
				.apply();
	}
}
