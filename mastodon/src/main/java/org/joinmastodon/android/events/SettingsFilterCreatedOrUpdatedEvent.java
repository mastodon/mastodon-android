package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Filter;

public class SettingsFilterCreatedOrUpdatedEvent{
	public final String accountID;
	public final Filter filter;

	public SettingsFilterCreatedOrUpdatedEvent(String accountID, Filter filter){
		this.accountID=accountID;
		this.filter=filter;
	}
}
