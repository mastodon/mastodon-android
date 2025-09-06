package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class RevokeStatusQuote extends MastodonAPIRequest<Status>{
	public RevokeStatusQuote(String id, String quoteID){
		super(HttpMethod.POST, "/statuses/"+id+"/quotes/"+quoteID+"/revoke", Status.class);
		setRequestBody(new Object());
	}
}
