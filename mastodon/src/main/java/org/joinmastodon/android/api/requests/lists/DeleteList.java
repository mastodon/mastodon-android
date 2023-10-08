package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

public class DeleteList extends ResultlessMastodonAPIRequest{
	public DeleteList(String id){
		super(HttpMethod.DELETE, "/lists/"+id);
	}
}
