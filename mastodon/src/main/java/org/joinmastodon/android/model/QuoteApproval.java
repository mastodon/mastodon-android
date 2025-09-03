package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.EnumSet;

@Parcel
public class QuoteApproval extends BaseModel{
	@RequiredField
	public EnumSet<Policy> automatic;
	public EnumSet<Policy> manual;
	@RequiredField
	public CurrentUserPolicy currentUser=CurrentUserPolicy.UNKNOWN;

	public StatusQuotePolicy toQuotePolicy(){
		if(automatic.contains(Policy.PUBLIC))
			return StatusQuotePolicy.PUBLIC;
		if(automatic.contains(Policy.FOLLOWERS))
			return StatusQuotePolicy.FOLLOWERS;
		return StatusQuotePolicy.NOBODY;
	}

	public enum Policy{
		@SerializedName("public")
		PUBLIC,
		@SerializedName("followers")
		FOLLOWERS,
		@SerializedName("unsupported_policy")
		UNSUPPORTED_POLICY
	}

	public enum CurrentUserPolicy{
		@SerializedName("automatic")
		AUTOMATIC,
		@SerializedName("manual")
		MANUAL,
		@SerializedName("denied")
		DENIED,
		@SerializedName("unknown")
		UNKNOWN
	}
}
