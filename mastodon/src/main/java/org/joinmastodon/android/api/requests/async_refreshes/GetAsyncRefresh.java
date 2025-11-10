package org.joinmastodon.android.api.requests.async_refreshes;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.AsyncRefresh;
import org.joinmastodon.android.model.BaseModel;

public class GetAsyncRefresh extends MastodonAPIRequest<GetAsyncRefresh.Response>{
	public GetAsyncRefresh(String id){
		super(HttpMethod.GET, "/async_refreshes/"+id, Response.class);
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v1_alpha";
	}

	public static class Response extends BaseModel{
		@RequiredField
		public AsyncRefresh asyncRefresh;
	}
}
