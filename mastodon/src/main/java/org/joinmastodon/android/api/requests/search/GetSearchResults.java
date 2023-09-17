package org.joinmastodon.android.api.requests.search;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.SearchResults;

public class GetSearchResults extends MastodonAPIRequest<SearchResults>{
	public GetSearchResults(String query, Type type, boolean resolve, String maxID, int offset, int count){
		super(HttpMethod.GET, "/search", SearchResults.class);
		addQueryParameter("q", query);
		if(type!=null)
			addQueryParameter("type", type.name().toLowerCase());
		if(resolve)
			addQueryParameter("resolve", "true");
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(offset>0)
			addQueryParameter("offset", String.valueOf(offset));
		if(count>0)
			addQueryParameter("limit", String.valueOf(count));
	}

	public GetSearchResults limit(int limit){
		addQueryParameter("limit", String.valueOf(limit));
		return this;
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
