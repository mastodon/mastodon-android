package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum StatusQuotePolicy{
	@SerializedName("public")
	PUBLIC,
	@SerializedName("followers")
	FOLLOWERS,
	@SerializedName("nobody")
	NOBODY;
}
