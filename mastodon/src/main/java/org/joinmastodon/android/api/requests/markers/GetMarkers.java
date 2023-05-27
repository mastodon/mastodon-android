package org.joinmastodon.android.api.requests.markers;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.TimelineMarkers;

public class GetMarkers extends MastodonAPIRequest<TimelineMarkers>{
	public GetMarkers(){
		super(HttpMethod.GET, "/markers", TimelineMarkers.class);
		addQueryParameter("timeline[]", "home");
		addQueryParameter("timeline[]", "notifications");
	}
}
