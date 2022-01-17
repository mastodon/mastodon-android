package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusCreatedEvent{
	public Status status;

	public StatusCreatedEvent(Status status){
		this.status=status;
	}
}
