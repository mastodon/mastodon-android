package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NotificationType;

import java.util.EnumSet;

public class GetUnreadNotificationsCount extends MastodonAPIRequest<GetUnreadNotificationsCount.Response>{
	public GetUnreadNotificationsCount(EnumSet<NotificationType> includeTypes, EnumSet<NotificationType> groupedTypes){
		super(HttpMethod.GET, "/notifications/unread_count", Response.class);
		if(includeTypes!=null){
			for(String type: ApiUtils.enumSetToStrings(includeTypes, NotificationType.class)){
				addQueryParameter("types[]", type);
			}
			for(String type:ApiUtils.enumSetToStrings(EnumSet.complementOf(includeTypes), NotificationType.class)){
				addQueryParameter("exclude_types[]", type);
			}
		}
		if(groupedTypes!=null){
			for(String type:ApiUtils.enumSetToStrings(groupedTypes, NotificationType.class)){
				addQueryParameter("grouped_types[]", type);
			}
		}
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}

	public static class Response{
		public int count;
	}
}
