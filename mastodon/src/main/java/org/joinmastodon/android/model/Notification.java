package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;

@Parcel
public class Notification extends BaseModel implements DisplayItemsParent{
	@RequiredField
	public String id;
//	@RequiredField
	public Type type;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Account account;

	public Status status;

	@Override
	public String getID(){
		return id;
	}

	public enum Type{
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
		STATUS
	}
}
