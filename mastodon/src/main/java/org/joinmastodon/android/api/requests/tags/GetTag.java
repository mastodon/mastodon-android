package org.joinmastodon.android.api.requests.tags;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

public class GetTag extends MastodonAPIRequest<Hashtag>{
	public GetTag(String tag){
		super(HttpMethod.GET, "/tags/"+tag, Hashtag.class);
	}
}
