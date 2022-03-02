package org.joinmastodon.android.api.requests.instance;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Instance;

public class GetInstance extends MastodonAPIRequest<Instance>{
	public GetInstance(){
		super(HttpMethod.GET, "/instance", Instance.class);
	}
}
