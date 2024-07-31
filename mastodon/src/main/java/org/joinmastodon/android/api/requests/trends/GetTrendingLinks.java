package org.joinmastodon.android.api.requests.trends;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Card;

import java.util.List;

public class GetTrendingLinks extends MastodonAPIRequest<List<Card>>{
	public GetTrendingLinks(int limit){
		super(HttpMethod.GET, "/trends/links", new TypeToken<>(){});
		addQueryParameter("limit", String.valueOf(limit));
	}
}
