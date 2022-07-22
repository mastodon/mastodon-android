package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class SetStatusPinned extends MastodonAPIRequest<Status>{
	public SetStatusPinned(String id, boolean pinned){
		super(HttpMethod.POST, "/statuses/"+id+"/"+(pinned ? "pin" : "unpin"), Status.class);
		setRequestBody(new Object());
	}
}
