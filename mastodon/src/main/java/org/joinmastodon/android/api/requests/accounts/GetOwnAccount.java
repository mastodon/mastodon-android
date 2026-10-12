package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class GetOwnAccount extends MastodonAPIRequest<Account>{
	public GetOwnAccount(){
		this(false);
	}

	public GetOwnAccount(boolean allowNonfunctional){
		super(HttpMethod.GET, "/accounts/verify_credentials", Account.class);
		if(allowNonfunctional)
			addQueryParameter("allow_nonfunctional", "true");
	}
}
