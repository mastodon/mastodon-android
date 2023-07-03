package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountBlocked extends MastodonAPIRequest<Relationship> {
    public SetAccountBlocked(String id, boolean blocked) {
        super(HttpMethod.POST, "/accounts/" + id + "/" + (blocked ? "block" : "unblock"), Relationship.class);
        setRequestBody(new Object());
    }
}
