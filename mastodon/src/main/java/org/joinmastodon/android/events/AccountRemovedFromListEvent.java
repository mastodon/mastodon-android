package org.joinmastodon.android.events;

public class AccountRemovedFromListEvent{
	public final String accountID;
	public final String listID;
	public final String targetAccountID;

	public AccountRemovedFromListEvent(String accountID, String listID, String targetAccountID){
		this.accountID=accountID;
		this.listID=listID;
		this.targetAccountID=targetAccountID;
	}
}
