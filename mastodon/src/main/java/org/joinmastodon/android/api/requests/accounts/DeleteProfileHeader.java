package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class DeleteProfileHeader extends MastodonAPIRequest<Account>{
	public DeleteProfileHeader(){
		super(HttpMethod.DELETE, "/profile/header", Account.class);
	}
}
