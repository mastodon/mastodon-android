package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.NotificationGroup;
import org.joinmastodon.android.model.Status;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotificationViewModel implements DisplayItemsParent{
	public NotificationGroup notification;
	public List<Account> accounts;
	public Status status;

	public static List<NotificationViewModel> makeNotificationViewModels(List<NotificationGroup> notifications, Map<String, Account> accounts, Map<String, Status> statuses){
		return notifications.stream()
				.filter(ng->ng.type!=null)
				.map(ng->{
					NotificationViewModel nvm=new NotificationViewModel();
					nvm.notification=ng;
					nvm.accounts=ng.sampleAccountIds.stream().map(accounts::get).filter(Objects::nonNull).collect(Collectors.toList());
					if(nvm.accounts.size()!=ng.sampleAccountIds.size())
						return null;
					if(ng.statusId!=null){
						nvm.status=statuses.get(ng.statusId);
						if(nvm.status==null)
							return null;
					}
					return nvm;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Override
	public String getID(){
		return notification.groupKey;
	}
}
