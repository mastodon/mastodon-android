package org.joinmastodon.android.api.requests.instance;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.InstanceV1;

public class GetInstanceV1 extends MastodonAPIRequest<InstanceV1>{
	public GetInstanceV1(){
		super(HttpMethod.GET, "/instance", InstanceV1.class);
	}
}
