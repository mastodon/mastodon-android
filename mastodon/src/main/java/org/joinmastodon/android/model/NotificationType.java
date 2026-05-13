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
	UPDATE,
	@SerializedName("severed_relationships")
	SEVERED_RELATIONSHIPS,
	@SerializedName("moderation_warning")
	MODERATION_WARNING,
	@SerializedName("quote")
	QUOTE,
	@SerializedName("quoted_update")
	QUOTED_UPDATE,
	@SerializedName("admin.sign_up")
	ADMIN_SIGNUP,
	@SerializedName("admin.report")
	ADMIN_REPORT,
	FALLBACK,
	@SerializedName("added_to_collection")
	ADDED_TO_COLLECTION,
	@SerializedName("collection_update")
	COLLECTION_UPDATE,
	;

	public boolean canBeGrouped(){
		return this==REBLOG || this==FAVORITE || this==FOLLOW;
	}

	public static EnumSet<NotificationType> getGroupableTypes(){
		return EnumSet.of(FAVORITE, REBLOG, FOLLOW);
	}

	public static EnumSet<NotificationType> getAllTypes(){
		EnumSet<NotificationType> types=EnumSet.allOf(NotificationType.class);
		types.remove(FALLBACK);
		return types;
	}
}
