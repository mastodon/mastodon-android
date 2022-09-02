package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusUpdatedEvent{
	public Status status;

	public StatusUpdatedEvent(Status status){
		this.status=status;
	}
}
