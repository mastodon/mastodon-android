package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NotificationsPolicy;

public class SetNotificationsPolicy extends MastodonAPIRequest<NotificationsPolicy>{
	public SetNotificationsPolicy(NotificationsPolicy policy){
		super(HttpMethod.PUT, "/notifications/policy", NotificationsPolicy.class);
		setRequestBody(policy);
	}
}
