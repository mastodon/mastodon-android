package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NotificationType;

import java.util.EnumSet;

public class GetUnreadNotificationsCount extends MastodonAPIRequest<GetUnreadNotificationsCount.Response>{
	public GetUnreadNotificationsCount(EnumSet<NotificationType> includeTypes, EnumSet<NotificationType> groupedTypes, EnumSet<NotificationType> excludeTypes){
		super(HttpMethod.GET, "/notifications/unread_count", Response.class);
		if(includeTypes!=null){
			for(String type: ApiUtils.enumSetToStrings(includeTypes)){
				addQueryParameter("types[]", type);
			}
		}
		if(groupedTypes!=null){
			for(String type:ApiUtils.enumSetToStrings(groupedTypes)){
				addQueryParameter("grouped_types[]", type);
			}
		}
		if(excludeTypes!=null){
			for(String type:ApiUtils.enumSetToStrings(excludeTypes)){
				addQueryParameter("exclude_types[]", type);
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
