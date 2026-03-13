package org.joinmastodon.android.api.requests.profile;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Profile;

public class GetProfile extends MastodonAPIRequest<Profile>{
	public GetProfile(){
		super(HttpMethod.GET, "/profile", Profile.class);
	}
}
