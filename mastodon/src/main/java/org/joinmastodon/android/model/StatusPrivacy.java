package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum StatusPrivacy{
	@SerializedName("public")
	PUBLIC,
	@SerializedName("unlisted")
	UNLISTED,
	@SerializedName("private")
	PRIVATE,
	@SerializedName("direct")
	DIRECT;
}
