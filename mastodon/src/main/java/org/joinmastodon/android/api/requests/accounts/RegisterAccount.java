package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Token;

public class RegisterAccount extends MastodonAPIRequest<Token>{
	public RegisterAccount(String username, String email, String password, String locale, String reason, String timezone){
		super(HttpMethod.POST, "/accounts", Token.class);
		setRequestBody(new Body(username, email, password, locale, reason, timezone));
	}

	private static class Body{
		public String username, email, password, locale, reason, timeZone;
		public boolean agreement=true;

		public Body(String username, String email, String password, String locale, String reason, String timeZone){
			this.username=username;
			this.email=email;
			this.password=password;
			this.locale=locale;
			this.reason=reason;
			this.timeZone=timeZone;
		}
	}
}
