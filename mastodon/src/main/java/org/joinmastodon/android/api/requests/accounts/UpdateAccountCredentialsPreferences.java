package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.StatusQuotePolicy;

public class UpdateAccountCredentialsPreferences extends MastodonAPIRequest<Account>{
	public UpdateAccountCredentialsPreferences(Preferences preferences, Boolean locked, Boolean discoverable, Boolean indexable){
		super(HttpMethod.PATCH, "/accounts/update_credentials", Account.class);
		setRequestBody(new Request(locked, discoverable, indexable, new RequestSource(preferences.postingDefaultVisibility, preferences.postingDefaultLanguage, preferences.postingDefaultQuotePolicy)));
	}

	private static class Request{
		public Boolean locked, discoverable, indexable;
		public RequestSource source;

		public Request(Boolean locked, Boolean discoverable, Boolean indexable, RequestSource source){
			this.locked=locked;
			this.discoverable=discoverable;
			this.indexable=indexable;
			this.source=source;
		}
	}

	private static class RequestSource{
		public StatusPrivacy privacy;
		public String language;
		public StatusQuotePolicy quotePolicy;

		public RequestSource(StatusPrivacy privacy, String language, StatusQuotePolicy quotePolicy){
			this.privacy=privacy;
			this.language=language;
			this.quotePolicy=quotePolicy;
		}
	}
}
