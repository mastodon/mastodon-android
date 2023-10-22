package org.joinmastodon.android.events;

import org.joinmastodon.android.model.FollowList;

public class ListCreatedEvent{
	public final String accountID;
	public final FollowList list;

	public ListCreatedEvent(String accountID, FollowList list){
		this.accountID=accountID;
		this.list=list;
	}
}
