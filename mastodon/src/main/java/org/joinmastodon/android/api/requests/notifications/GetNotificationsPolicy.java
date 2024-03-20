package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NotificationsPolicy;

public class GetNotificationsPolicy extends MastodonAPIRequest<NotificationsPolicy>{
	public GetNotificationsPolicy(){
		super(HttpMethod.GET, "/notifications/policy", NotificationsPolicy.class);
	}
}
