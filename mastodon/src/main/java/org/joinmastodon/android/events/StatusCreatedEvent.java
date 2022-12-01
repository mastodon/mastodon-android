package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusCreatedEvent{
	public final Status status;
	public final String accountID;

	public StatusCreatedEvent(Status status, String accountID){
		this.status=status;
		this.accountID=accountID;
	}
}
