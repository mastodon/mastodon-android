package org.joinmastodon.android.api;

public abstract class ResultlessMastodonAPIRequest extends MastodonAPIRequest<Void> {
    public ResultlessMastodonAPIRequest(HttpMethod method, String path) {
        super(method, path, (Class<Void>) null);
    }
}
