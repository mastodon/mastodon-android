package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusCountersUpdatedEvent{
	public String id;
	public int favorites, reblogs;
	public boolean favorited, reblogged;

	public StatusCountersUpdatedEvent(Status s){
		id=s.id;
		favorites=s.favouritesCount;
		reblogs=s.reblogsCount;
		favorited=s.favourited;
		reblogged=s.reblogged;
	}
}
