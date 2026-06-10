package org.joinmastodon.android.api.requests.collections;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.collections.AccountCollections;

public class GetAccountCollections extends MastodonAPIRequest<AccountCollections>{
	public GetAccountCollections(String accountID, int offset, int limit){
		super(HttpMethod.GET, "/accounts/"+accountID+"/collections", AccountCollections.class);
		addQueryParameter("offset", offset+"");
		addQueryParameter("limit", limit+"");
	}
}
