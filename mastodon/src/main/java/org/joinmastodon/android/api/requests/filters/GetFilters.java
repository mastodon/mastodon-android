package org.joinmastodon.android.api.requests.filters;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Filter;

import java.util.List;

public class GetFilters extends MastodonAPIRequest<List<Filter>>{
	public GetFilters(){
		super(HttpMethod.GET, "/filters", new TypeToken<>(){});
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}
}
