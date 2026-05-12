package org.joinmastodon.android.api.requests.collections;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

public class RevokeCollectionItem extends ResultlessMastodonAPIRequest{
	public RevokeCollectionItem(String collectionID, String itemID){
		super(HttpMethod.POST, "/collections/"+collectionID+"/items/"+itemID+"/revoke");
		setRequestBody(new Object());
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v1_alpha";
	}
}
