package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

public class RespondToNotificationRequest extends ResultlessMastodonAPIRequest{
	public RespondToNotificationRequest(String id, boolean allow){
		super(HttpMethod.POST, "/notifications/requests/"+id+(allow ? "/accept" : "/dismiss"));
		setRequestBody(new Object());
	}
}
