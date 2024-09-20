package org.joinmastodon.android.model;

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
	// TODO report
	// TODO event
	// TODO moderation_warning
}
