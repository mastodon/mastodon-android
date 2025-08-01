package org.joinmastodon.android.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.Collection;
import java.util.List;

public class GetStatusesByIDs extends MastodonAPIRequest<List<Status>>{
	public GetStatusesByIDs(Collection<String> ids){
		super(HttpMethod.GET, "/statuses", new TypeToken<>(){});
		for(String id:ids)
			addQueryParameter("id[]", id);
	}
}
