package org.joinmastodon.android.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.Status;

public class GetListTimeline extends HeaderPaginationRequest<Status>{
	public GetListTimeline(String listID, String maxID, String minID, int limit, String sinceID){
		super(HttpMethod.GET, "/timelines/list/"+listID, new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(sinceID!=null)
			addQueryParameter("since_id", sinceID);
		removeUnsupportedItems=true;
	}
}
