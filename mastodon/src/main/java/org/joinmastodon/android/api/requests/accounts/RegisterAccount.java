package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Token;

public class RegisterAccount extends MastodonAPIRequest<Token>{
	public RegisterAccount(String username, String email, String password, String locale, String reason, String timezone, String inviteCode, String dateOfBirth){
		super(HttpMethod.POST, "/accounts", Token.class);
		setRequestBody(new Body(username, email, password, locale, reason, timezone, inviteCode, dateOfBirth));
	}

	private static class Body{
		public String username, email, password, locale, reason, timeZone, inviteCode, dateOfBirth;
		public boolean agreement=true;

		public Body(String username, String email, String password, String locale, String reason, String timeZone, String inviteCode, String dateOfBirth){
			this.username=username;
			this.email=email;
			this.password=password;
			this.locale=locale;
			this.reason=reason;
			this.timeZone=timeZone;
			this.inviteCode=inviteCode;
			this.dateOfBirth=dateOfBirth;
		}
	}
}
