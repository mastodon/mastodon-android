package org.joinmastodon.android.model;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.RequiredField;

public non-sealed class PartialAccount extends BaseModel implements AccountOrPartial{
	@RequiredField
	public String id;
	@RequiredField
	public String acct;
	public boolean bot;
	public boolean locked;
	@RequiredField
	public String url;
	public String avatar;
	public String avatarStatic;
	public String avatarDescription;

	@Override
	public String getID(){
		return id;
	}

	@Override
	public String getAvatar(){
		return GlobalUserPreferences.playGifs ? avatar : avatarStatic;
	}

	@Override
	public String getURL(){
		return url;
	}

	@Override
	public String getUsername(){
		return acct;
	}
}
