package org.joinmastodon.android.api.requests.instance;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.InstanceV2;

public class GetInstanceV2 extends MastodonAPIRequest<InstanceV2>{
	public GetInstanceV2(){
		super(HttpMethod.GET, "/instance", InstanceV2.class);
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}
}
