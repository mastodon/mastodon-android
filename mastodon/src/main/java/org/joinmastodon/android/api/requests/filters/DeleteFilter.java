package org.joinmastodon.android.api.requests.filters;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

public class DeleteFilter extends ResultlessMastodonAPIRequest{
	public DeleteFilter(String id){
		super(HttpMethod.DELETE, "/filters/"+id);
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}
}
