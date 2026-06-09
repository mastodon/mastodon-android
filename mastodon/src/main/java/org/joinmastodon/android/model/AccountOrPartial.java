package org.joinmastodon.android.model;

public sealed interface AccountOrPartial permits Account, PartialAccount{
	String getID();
	String getAvatar();
	String getURL();
	String getUsername();
}
