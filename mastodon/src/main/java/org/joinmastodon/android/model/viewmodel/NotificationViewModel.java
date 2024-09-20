package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.NotificationGroup;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class NotificationViewModel implements DisplayItemsParent{
	public NotificationGroup notification;
	public List<Account> accounts;
	public Status status;

	@Override
	public String getID(){
		return notification.groupKey;
	}
}
