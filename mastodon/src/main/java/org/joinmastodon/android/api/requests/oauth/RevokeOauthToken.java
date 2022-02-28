package org.joinmastodon.android.api.requests.oauth;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class RevokeOauthToken extends MastodonAPIRequest<Object>{
	public RevokeOauthToken(String clientID, String clientSecret, String token){
		super(HttpMethod.POST, "/oauth/revoke", Object.class);
		setRequestBody(new Body(clientID, clientSecret, token));
	}

	@Override
	protected String getPathPrefix(){
		return "";
	}

	private static class Body{
		public String clientId, clientSecret, token;

		public Body(String clientId, String clientSecret, String token){
			this.clientId=clientId;
			this.clientSecret=clientSecret;
			this.token=token;
		}
	}
}
