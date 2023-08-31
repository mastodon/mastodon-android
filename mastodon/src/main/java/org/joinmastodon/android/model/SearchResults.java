package org.joinmastodon.android.model;

import java.util.List;

public class SearchResults extends BaseModel{
	public List<Account> accounts;
	public List<Status> statuses;
	public List<Hashtag> hashtags;
}
