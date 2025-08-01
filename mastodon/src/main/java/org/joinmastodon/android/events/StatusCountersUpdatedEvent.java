package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Status;

public class StatusCountersUpdatedEvent{
	public String id;
	public long favorites, reblogs, replies;
	public boolean favorited, reblogged, bookmarked;
	public final CounterType type;

	public StatusCountersUpdatedEvent(Status s, CounterType type){
		id=s.id;
		favorites=s.favouritesCount;
		favorited=s.favourited;
		reblogs=s.reblogsCount;
		reblogged=s.reblogged;
		replies=s.repliesCount;
		bookmarked=s.bookmarked;
		this.type=type;
	}

	public enum CounterType{
		FAVORITES,
		REBLOGS,
		REPLIES,
		BOOKMARKS
	}
}
