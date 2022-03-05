package org.joinmastodon.android.api.requests.notifications;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Notification;

import java.util.EnumSet;
import java.util.List;

public class GetNotifications extends MastodonAPIRequest<List<Notification>>{
	public GetNotifications(String maxID, int limit, EnumSet<Notification.Type> excludeTypes){
		super(HttpMethod.GET, "/notifications", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(excludeTypes!=null){
			for(Notification.Type nt:excludeTypes){
				try{
					addQueryParameter("exclude_types[]", nt.getDeclaringClass().getField(nt.name()).getAnnotation(SerializedName.class).value());
				}catch(NoSuchFieldException ignore){}
			}
		}
	}
}
