package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Attachment;

public class UpdateAttachment extends MastodonAPIRequest<Attachment> {
    public UpdateAttachment(String id, String description) {
        super(HttpMethod.PUT, "/media/" + id, Attachment.class);
        setRequestBody(new Body(description));
    }

    private static class Body {
        public String description;

        public Body(String description) {
            this.description = description;
        }
    }
}
