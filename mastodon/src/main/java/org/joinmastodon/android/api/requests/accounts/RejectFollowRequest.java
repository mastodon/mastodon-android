package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class RejectFollowRequest extends MastodonAPIRequest<Relationship> {
	public RejectFollowRequest(String accountID) {
		super(HttpMethod.POST, "/follow_requests/" + accountID + "/reject", Relationship.class);
		setRequestBody(new Object());
	}
}
