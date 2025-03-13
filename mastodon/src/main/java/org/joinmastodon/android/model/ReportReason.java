package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum ReportReason{
	PERSONAL,
	@SerializedName("spam")
	SPAM,
	@SerializedName("legal")
	LEGAL,
	@SerializedName("violation")
	VIOLATION,
	@SerializedName("other")
	OTHER
}
