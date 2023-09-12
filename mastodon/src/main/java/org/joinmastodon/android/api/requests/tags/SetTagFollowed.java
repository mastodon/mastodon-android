package org.joinmastodon.android.api.requests.tags;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

public class SetTagFollowed extends MastodonAPIRequest<Hashtag>{
	public SetTagFollowed(String tag, boolean followed){
		super(HttpMethod.POST, "/tags/"+tag+(followed ? "/follow" : "/unfollow"), Hashtag.class);
		setRequestBody(new Object());
	}
}
