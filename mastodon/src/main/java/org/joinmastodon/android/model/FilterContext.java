package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.R;

import androidx.annotation.StringRes;

public enum FilterContext{
	@SerializedName("home")
	HOME,
	@SerializedName("notifications")
	NOTIFICATIONS,
	@SerializedName("public")
	PUBLIC,
	@SerializedName("thread")
	THREAD,
	@SerializedName("account")
	ACCOUNT;

	@StringRes
	public int getDisplayNameRes(){
		return switch(this){
			case HOME -> R.string.filter_context_home_lists;
			case NOTIFICATIONS -> R.string.filter_context_notifications;
			case PUBLIC -> R.string.filter_context_public_timelines;
			case THREAD -> R.string.filter_context_threads_replies;
			case ACCOUNT -> R.string.filter_context_profiles;
		};
	}
}
