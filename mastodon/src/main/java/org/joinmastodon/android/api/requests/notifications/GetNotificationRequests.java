package org.joinmastodon.android.api.requests.notifications;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.NotificationRequest;

public class GetNotificationRequests extends HeaderPaginationRequest<NotificationRequest>{
	public GetNotificationRequests(String maxID){
		super(HttpMethod.GET, "/notifications/requests", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
	}
}
