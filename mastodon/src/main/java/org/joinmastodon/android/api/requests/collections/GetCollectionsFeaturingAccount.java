package org.joinmastodon.android.api.requests.collections;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.PaginationHeaders;
import org.joinmastodon.android.model.collections.AccountCollections;

import java.io.IOException;

import okhttp3.Response;

public class GetCollectionsFeaturingAccount extends MastodonAPIRequest<AccountCollections>{
	public GetCollectionsFeaturingAccount(String accountID, int offset, int limit){
		super(HttpMethod.GET, "/accounts/"+accountID+"/in_collections", AccountCollections.class);
		addQueryParameter("offset", offset+"");
		addQueryParameter("limit", limit+"");
	}

	@Override
	public void validateAndPostprocessResponse(AccountCollections respObj, Response httpResponse) throws IOException{
		super.validateAndPostprocessResponse(respObj, httpResponse);
		PaginationHeaders urls=ApiUtils.parsePaginationHeaders(httpResponse);
		respObj.hasNextPage=urls.next()!=null;
	}
}
