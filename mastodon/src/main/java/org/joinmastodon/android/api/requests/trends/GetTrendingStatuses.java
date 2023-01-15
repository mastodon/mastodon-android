package org.joinmastodon.android.api.requests.trends;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetTrendingStatuses extends MastodonAPIRequest<List<Status>>{
	public GetTrendingStatuses(int offset, int limit){
		super(HttpMethod.GET, "/trends/statuses", new TypeToken<>(){});
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(offset>0)
			addQueryParameter("offset", ""+offset);
	}
}
