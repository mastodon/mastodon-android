package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class DeleteStatus extends MastodonAPIRequest<Status> {
    public DeleteStatus(String id) {
        super(HttpMethod.DELETE, "/statuses/" + id, Status.class);
    }
}
