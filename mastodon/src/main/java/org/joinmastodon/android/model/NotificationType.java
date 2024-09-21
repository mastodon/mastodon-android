package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.EnumSet;

public enum NotificationType{
	@SerializedName("follow")
	FOLLOW,
	@SerializedName("follow_request")
	FOLLOW_REQUEST,
	@SerializedName("mention")
	MENTION,
	@SerializedName("reblog")
	REBLOG,
	@SerializedName("favourite")
	FAVORITE,
	@SerializedName("poll")
	POLL,
	@SerializedName("status")
	STATUS,
	@SerializedName("update")
	UPDATE;

	public boolean canBeGrouped(){
		return this==REBLOG || this==FAVORITE;
	}

	public static EnumSet<NotificationType> getGroupableTypes(){
		return EnumSet.of(FAVORITE, REBLOG);
	}
}
