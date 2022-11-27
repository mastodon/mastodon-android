package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusCountersUpdatedEvent{
	public String id;
	public long favorites, reblogs, replies;
	public boolean favorited, reblogged, bookmarked;

	public StatusCountersUpdatedEvent(Status s){
		id=s.id;
		favorites=s.favouritesCount;
		reblogs=s.reblogsCount;
		replies=s.repliesCount;
		favorited=s.favourited;
		reblogged=s.reblogged;
		bookmarked=s.bookmarked;
	}
}
