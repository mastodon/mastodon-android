package org.joinmastodon.android.events;

public class StatusDeletedEvent{
	public final String id;
	public final String accountID;

	public StatusDeletedEvent(String id, String accountID){
		this.id=id;
		this.accountID=accountID;
	}
}
