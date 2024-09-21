package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;

@Parcel
public class RelationshipSeveranceEvent extends BaseModel{
	public String id;
	@RequiredField
	public Type type;
	public boolean purged;
	@RequiredField
	public String targetName;
	public int followersCount;
	public int followingCount;
	public Instant createdAt;

	public enum Type{
		@SerializedName("domain_block")
		DOMAIN_BLOCK,
		@SerializedName("user_domain_block")
		USER_DOMAIN_BLOCK,
		@SerializedName("account_suspension")
		ACCOUNT_SUSPENSION
	}
}
