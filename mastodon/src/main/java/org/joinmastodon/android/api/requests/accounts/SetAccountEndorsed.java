package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountEndorsed extends MastodonAPIRequest<Relationship>{
	public SetAccountEndorsed(String id, boolean endorsed){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(endorsed ? "pin" : "unpin"), Relationship.class);
		setRequestBody(new Object());
	}
}
