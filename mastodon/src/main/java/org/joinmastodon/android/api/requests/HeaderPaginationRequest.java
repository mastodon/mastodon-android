package org.joinmastodon.android.api.requests;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.PaginationHeaders;
import org.joinmastodon.android.model.HeaderPaginationList;

import java.io.IOException;

import okhttp3.Response;

public abstract class HeaderPaginationRequest<I> extends MastodonAPIRequest<HeaderPaginationList<I>>{

	public HeaderPaginationRequest(HttpMethod method, String path, Class<HeaderPaginationList<I>> respClass){
		super(method, path, respClass);
	}

	public HeaderPaginationRequest(HttpMethod method, String path, TypeToken<HeaderPaginationList<I>> respTypeToken){
		super(method, path, respTypeToken);
	}

	@Override
	public void validateAndPostprocessResponse(HeaderPaginationList<I> respObj, Response httpResponse) throws IOException{
		super.validateAndPostprocessResponse(respObj, httpResponse);
		PaginationHeaders urls=ApiUtils.parsePaginationHeaders(httpResponse);
		respObj.nextPageUri=urls.next();
		respObj.prevPageUri=urls.prev();
	}
}
