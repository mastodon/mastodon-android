package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountFollowed extends MastodonAPIRequest<Relationship>{
	public SetAccountFollowed(String id, boolean followed){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(followed ? "follow" : "unfollow"), Relationship.class);
		setRequestBody(new Object());
	}
}
