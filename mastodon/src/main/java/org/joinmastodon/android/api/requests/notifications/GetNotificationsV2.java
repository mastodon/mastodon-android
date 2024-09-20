package org.joinmastodon.android.api.requests.notifications;

import android.text.TextUtils;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.NotificationGroup;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.Status;

import java.util.EnumSet;
import java.util.List;

public class GetNotificationsV2 extends MastodonAPIRequest<GetNotificationsV2.GroupedNotificationsResults>{
	public GetNotificationsV2(String maxID, int limit, EnumSet<NotificationType> includeTypes, EnumSet<NotificationType> groupedTypes){
		this(maxID, limit, includeTypes, groupedTypes, null);
	}

	public GetNotificationsV2(String maxID, int limit, EnumSet<NotificationType> includeTypes, EnumSet<NotificationType> groupedTypes, String onlyAccountID){
		super(HttpMethod.GET, "/notifications", GroupedNotificationsResults.class);
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(includeTypes!=null){
			for(String type:ApiUtils.enumSetToStrings(includeTypes, NotificationType.class)){
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
		if(!TextUtils.isEmpty(onlyAccountID))
			addQueryParameter("account_id", onlyAccountID);
		removeUnsupportedItems=true;
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}

	@AllFieldsAreRequired
	public static class GroupedNotificationsResults extends BaseModel{
		public List<Account> accounts;
		public List<Status> statuses;
		public List<NotificationGroup> notificationGroups;

		@Override
		public void postprocess() throws ObjectValidationException{
			super.postprocess();
			for(Account acc:accounts)
				acc.postprocess();
			for(Status s:statuses)
				s.postprocess();
			for(NotificationGroup ng:notificationGroups)
				ng.postprocess();
		}
	}
}
