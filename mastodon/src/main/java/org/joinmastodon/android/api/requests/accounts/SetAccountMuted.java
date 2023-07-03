package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountMuted extends MastodonAPIRequest<Relationship> {
    public SetAccountMuted(String id, boolean muted) {
        super(HttpMethod.POST, "/accounts/" + id + "/" + (muted ? "mute" : "unmute"), Relationship.class);
        setRequestBody(new Object());
    }
}
