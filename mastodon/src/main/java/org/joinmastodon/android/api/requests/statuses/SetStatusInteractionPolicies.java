package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusQuotePolicy;

import java.util.Map;

public class SetStatusInteractionPolicies extends MastodonAPIRequest<Status>{
	public SetStatusInteractionPolicies(String id, StatusQuotePolicy quotePolicy){
		super(HttpMethod.PUT, "/statuses/"+id+"/interaction_policy", Status.class);
		setRequestBody(Map.of("quote_approval_policy", quotePolicy));
	}
}
