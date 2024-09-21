package org.joinmastodon.android.model;

import android.util.Log;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;

import java.time.Instant;
import java.util.List;

public class NotificationGroup extends BaseModel{
	@RequiredField
	public String groupKey;
	public int notificationsCount;
	public NotificationType type;
	@RequiredField
	public String mostRecentNotificationId;
	public String pageMinId;
	public String pageMaxId;
	public Instant latestPageNotificationAt;
	@RequiredField
	public List<String> sampleAccountIds;
	public String statusId;
	public RelationshipSeveranceEvent event;
	public AccountWarning moderationWarning;


	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(event!=null){
			try{
				event.postprocess();
			}catch(ObjectValidationException x){
				Log.w("Notification", x);
				event=null;
			}
		}
		if(moderationWarning!=null)
			moderationWarning.postprocess();
		if(type!=NotificationType.SEVERED_RELATIONSHIPS && type!=NotificationType.MODERATION_WARNING && sampleAccountIds.isEmpty()){
			throw new ObjectValidationException("sample_account_ids must be present for type "+type);
		}
	}
}
