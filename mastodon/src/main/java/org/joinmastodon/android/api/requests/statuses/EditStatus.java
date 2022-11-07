package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class EditStatus extends MastodonAPIRequest<Status>{
	public EditStatus(CreateStatus.Request req, String id){
		super(HttpMethod.PUT, "/statuses/"+id, Status.class);
		setRequestBody(req);
	}
}
