package org.joinmastodon.android.model;

public class NotificationsPolicy extends BaseModel{
	public boolean filterNewAccounts;
	public boolean filterNotFollowers;
	public boolean filterNotFollowing;
	public boolean filterPrivateMentions;
	public Summary summary;

	public static class Summary{
		public int pendingNotificationsCount;
		public int pendingRequestsCount;
	}
}
