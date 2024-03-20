package org.joinmastodon.android.events;

public class NotificationRequestRespondedEvent{
	public final String accountID, requestID;

	public NotificationRequestRespondedEvent(String accountID, String requestID){
		this.accountID=accountID;
		this.requestID=requestID;
	}
}
