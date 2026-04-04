package org.joinmastodon.android.api.requests.profile;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Profile;

import java.util.Map;

public class UpdateProfile extends MastodonAPIRequest<Profile>{
	public UpdateProfile(boolean showMedia, boolean showMediaReplies, boolean showFeatured){
		super(HttpMethod.PUT, "/profile", Profile.class);
		setRequestBody(Map.of(
				"show_media", showMedia,
				"show_media_replies", showMediaReplies,
				"show_featured", showFeatured
		));
	}
}
