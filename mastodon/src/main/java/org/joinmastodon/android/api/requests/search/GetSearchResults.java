package org.joinmastodon.android.api.requests.search;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.SearchResults;

public class GetSearchResults extends MastodonAPIRequest<SearchResults>{
	public GetSearchResults(String query, Type type){
		super(HttpMethod.GET, "/search", SearchResults.class);
		addQueryParameter("q", query);
		if(type!=null)
			addQueryParameter("type", type.name().toLowerCase());
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}

	public enum Type{
		ACCOUNTS,
		HASHTAGS,
		STATUSES
	}
}
