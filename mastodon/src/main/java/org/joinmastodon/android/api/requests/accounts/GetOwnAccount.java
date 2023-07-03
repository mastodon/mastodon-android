package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class GetOwnAccount extends MastodonAPIRequest<Account> {
    public GetOwnAccount() {
        super(HttpMethod.GET, "/accounts/verify_credentials", Account.class);
    }
}
