package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class GetAccountByID extends MastodonAPIRequest<Account> {
    public GetAccountByID(String id) {
        super(HttpMethod.GET, "/accounts/" + id, Account.class);
    }
}
