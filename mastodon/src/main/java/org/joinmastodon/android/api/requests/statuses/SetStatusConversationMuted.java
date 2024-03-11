package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class SetStatusConversationMuted extends MastodonAPIRequest<Status>{
	public SetStatusConversationMuted(String id, boolean muted){
		super(HttpMethod.POST, "/statuses/"+id+(muted ? "/mute" : "/unmute"), Status.class);
		setRequestBody(new Object());
	}
}
