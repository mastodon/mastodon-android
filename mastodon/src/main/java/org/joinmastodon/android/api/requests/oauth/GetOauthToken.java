package org.joinmastodon.android.api.requests.oauth;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Token;

public class GetOauthToken extends MastodonAPIRequest<Token>{
	public GetOauthToken(String clientID, String clientSecret, String code, GrantType grantType){
		super(HttpMethod.POST, "/oauth/token", Token.class);
		setRequestBody(new Request(clientID, clientSecret, code, grantType,
				grantType==GrantType.CLIENT_CREDENTIALS ? AccountSessionManager.SCOPE : null,
				grantType==GrantType.CLIENT_CREDENTIALS ? null : AccountSessionManager.REDIRECT_URI));
	}

	@Override
	protected String getPathPrefix(){
		return "";
	}

	private static class Request{
		public GrantType grantType;
		public String clientId;
		public String clientSecret;
		public String redirectUri;
		public String scope;
		public String code;

		public Request(String clientId, String clientSecret, String code, GrantType grantType, String scope, String redirectUri){
			this.clientId=clientId;
			this.clientSecret=clientSecret;
			this.code=code;
			this.grantType=grantType;
			this.scope=scope;
			this.redirectUri=redirectUri;
		}
	}

	public enum GrantType{
		@SerializedName("authorization_code")
		AUTHORIZATION_CODE,
		@SerializedName("client_credentials")
		CLIENT_CREDENTIALS
	}
}
