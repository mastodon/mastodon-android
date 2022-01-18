package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetAccountStatuses extends MastodonAPIRequest<List<Status>>{
	public GetAccountStatuses(String id, String maxID, String minID, int limit){
		super(HttpMethod.GET, "/accounts/"+id+"/statuses", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
	}
}
