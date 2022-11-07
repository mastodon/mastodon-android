package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum StatusPrivacy{
	@SerializedName("public")
	PUBLIC(0),
	@SerializedName("unlisted")
	UNLISTED(1),
	@SerializedName("private")
	PRIVATE(2),
	@SerializedName("direct")
	DIRECT(3);

	private int privacy;

	StatusPrivacy(int privacy) {
		this.privacy = privacy;
	}

	public boolean isLessVisibleThan(StatusPrivacy other) {
		return privacy > other.getPrivacy();
	}

	public int getPrivacy() {
		return privacy;
	}
}
