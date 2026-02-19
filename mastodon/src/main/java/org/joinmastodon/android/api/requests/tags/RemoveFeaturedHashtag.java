package org.joinmastodon.android.api.requests.tags;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

public class RemoveFeaturedHashtag extends ResultlessMastodonAPIRequest{
	public RemoveFeaturedHashtag(String id){
		super(HttpMethod.DELETE, "/featured_tags/"+id);
	}
}
