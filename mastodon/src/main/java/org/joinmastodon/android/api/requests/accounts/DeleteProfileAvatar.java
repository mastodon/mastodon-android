package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class DeleteProfileAvatar extends MastodonAPIRequest<Account>{
	public DeleteProfileAvatar(){
		super(HttpMethod.DELETE, "/profile/avatar", Account.class);
	}
}
