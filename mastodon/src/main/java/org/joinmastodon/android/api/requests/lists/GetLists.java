package org.joinmastodon.android.api.requests.lists;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

import java.util.List;

public class GetLists extends MastodonAPIRequest<List<ListTimeline>>{
    public GetLists() {
        super(HttpMethod.GET, "/lists", new TypeToken<>(){});
    }
}
