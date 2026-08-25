package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountFollowed extends MastodonAPIRequest<Relationship>{
	public SetAccountFollowed(String id, boolean followed, boolean showReblogs, boolean notify, String ref){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(followed ? "follow" : "unfollow"), Relationship.class);
		if(followed){
			setRequestBody(new Request(showReblogs, notify, ref));
		}else{
			setRequestBody(new Object());
		}
	}

	private static class Request{
		public Boolean reblogs, notify;
		public String ref;

		public Request(Boolean reblogs, Boolean notify, String ref){
			this.reblogs=reblogs;
			this.notify=notify;
			this.ref=ref;
		}
	}
}
