package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.AsyncRefreshHeader;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.StatusContext;

import java.io.IOException;

import okhttp3.Response;

public class GetStatusContext extends MastodonAPIRequest<StatusContext>{
	public GetStatusContext(String id){
		super(HttpMethod.GET, "/statuses/"+id+"/context", StatusContext.class);
	}

	@Override
	public void validateAndPostprocessResponse(StatusContext respObj, Response httpResponse) throws IOException{
		super.validateAndPostprocessResponse(respObj, httpResponse);
		respObj.asyncRefresh=AsyncRefreshHeader.fromHttpResponse(httpResponse);
	}
}
