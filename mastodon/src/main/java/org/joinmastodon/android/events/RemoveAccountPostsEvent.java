package org.joinmastodon.android.events;

public class RemoveAccountPostsEvent{
	public final String accountID;
	public final String postsByAccountID;
	public final boolean isUnfollow;

	public RemoveAccountPostsEvent(String accountID, String postsByAccountID, boolean isUnfollow){
		this.accountID=accountID;
		this.postsByAccountID=postsByAccountID;
		this.isUnfollow=isUnfollow;
	}
}
