package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountFollowed extends MastodonAPIRequest<Relationship>{
	public SetAccountFollowed(String id, boolean followed, boolean showReblogs, boolean notify){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(followed ? "follow" : "unfollow"), Relationship.class);
		if(followed)
			setRequestBody(new Request(showReblogs, notify));
		else
			setRequestBody(new Object());
	}

	private static class Request{
		public Boolean reblogs, notify;

		public Request(Boolean reblogs, Boolean notify){
			this.reblogs=reblogs;
			this.notify=notify;
		}
	}
}
