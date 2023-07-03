package org.joinmastodon.android.api.requests.trends;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

import java.util.List;

public class GetTrendingHashtags extends MastodonAPIRequest<List<Hashtag>>{
	public GetTrendingHashtags(int limit){
		super(HttpMethod.GET, "/trends", new TypeToken<>(){});
		addQueryParameter("limit", String.valueOf(limit));
	}
}
