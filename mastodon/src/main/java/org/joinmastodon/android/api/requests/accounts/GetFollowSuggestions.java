package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowSuggestion;

import java.util.List;

public class GetFollowSuggestions extends MastodonAPIRequest<List<FollowSuggestion>> {
    public GetFollowSuggestions(int limit) {
        super(HttpMethod.GET, "/suggestions", new TypeToken<>() {});
        addQueryParameter("limit", String.valueOf(limit));
    }

    @Override
    protected String getPathPrefix() {
        return "/api/v2";
    }
}
