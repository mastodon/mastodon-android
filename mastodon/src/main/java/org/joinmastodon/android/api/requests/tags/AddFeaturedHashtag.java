package org.joinmastodon.android.api.requests.tags;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

import java.util.Map;

public class AddFeaturedHashtag extends MastodonAPIRequest<Hashtag>{
	public AddFeaturedHashtag(String tag){
		super(HttpMethod.POST, "/featured_tags", Hashtag.class);
		setRequestBody(Map.of("name", tag));
	}
}
