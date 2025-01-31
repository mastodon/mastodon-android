package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class AcceptFollowRequest extends MastodonAPIRequest<Relationship> {
	public AcceptFollowRequest(String accountID) {
		super(HttpMethod.POST, "/follow_requests/" + accountID + "/authorize", Relationship.class);
		setRequestBody(new Object());
	}
}
