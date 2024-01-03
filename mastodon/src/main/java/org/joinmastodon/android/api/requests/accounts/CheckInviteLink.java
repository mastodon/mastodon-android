package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;

public class CheckInviteLink extends MastodonAPIRequest<CheckInviteLink.Response>{
	public CheckInviteLink(String path){
		super(HttpMethod.GET, path, Response.class);
		addHeader("Accept", "application/json");
	}

	@Override
	protected String getPathPrefix(){
		return "";
	}

	public static class Response extends BaseModel{
		@RequiredField
		public String inviteCode;
	}
}
