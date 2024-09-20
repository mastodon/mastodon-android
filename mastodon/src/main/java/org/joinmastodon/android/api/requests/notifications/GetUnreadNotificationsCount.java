package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class GetUnreadNotificationsCount extends MastodonAPIRequest<GetUnreadNotificationsCount.Response>{
	public GetUnreadNotificationsCount(){
		super(HttpMethod.GET, "/notifications/unread_count", Response.class);
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}

	public static class Response{
		public int count;
	}
}
