package org.joinmastodon.android.api.requests.collections;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.collections.CollectionWithAccounts;

public class GetCollection extends MastodonAPIRequest<CollectionWithAccounts>{
	public GetCollection(String id){
		super(HttpMethod.GET, "/collections/"+id, CollectionWithAccounts.class);
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v1_alpha";
	}
}
