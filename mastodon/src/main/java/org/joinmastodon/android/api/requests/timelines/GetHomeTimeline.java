package org.joinmastodon.android.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.Status;

public class GetHomeTimeline extends HeaderPaginationRequest<Status>{
	public GetHomeTimeline(String maxID, String minID, int limit, String sinceID, boolean excludeReblogs, boolean excludeQuotes, boolean excludeReplies){
		super(HttpMethod.GET, "/timelines/home", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(sinceID!=null)
			addQueryParameter("since_id", sinceID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(excludeReblogs)
			addQueryParameter("exclude_reblogs", "true");
		if(excludeQuotes)
			addQueryParameter("exclude_quotes", "true");
		if(excludeReplies)
			addQueryParameter("exclude_replies", "true");
		removeUnsupportedItems=true;
	}
}
