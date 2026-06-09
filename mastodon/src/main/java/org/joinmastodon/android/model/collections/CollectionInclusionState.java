package org.joinmastodon.android.model.collections;

import com.google.gson.annotations.SerializedName;

public enum CollectionInclusionState{
	@SerializedName("pending")
	PENDING,
	@SerializedName("accepted")
	ACCEPTED,
	@SerializedName("rejected")
	REJECTED,
	@SerializedName("revoked")
	REVOKED,
}
