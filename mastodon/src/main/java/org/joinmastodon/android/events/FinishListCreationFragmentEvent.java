package org.joinmastodon.android.events;

public class FinishListCreationFragmentEvent{
	public final String accountID;
	public final String listID;

	public FinishListCreationFragmentEvent(String accountID, String listID){
		this.accountID=accountID;
		this.listID=listID;
	}
}
