package org.joinmastodon.android.events;

public class NotificationsMarkerUpdatedEvent{
	public final String accountID;
	public final String marker;
	public final boolean clearUnread;

	public NotificationsMarkerUpdatedEvent(String accountID, String marker, boolean clearUnread){
		this.accountID=accountID;
		this.marker=marker;
		this.clearUnread=clearUnread;
	}
}
