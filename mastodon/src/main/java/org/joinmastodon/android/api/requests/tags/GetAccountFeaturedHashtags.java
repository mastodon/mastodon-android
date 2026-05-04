package org.joinmastodon.android.api.requests.tags;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

import java.util.List;

public class GetAccountFeaturedHashtags extends MastodonAPIRequest<List<Hashtag>>{
	public GetAccountFeaturedHashtags(String id){
		super(HttpMethod.GET, "/accounts/"+id+"/featured_tags", new TypeToken<>(){});
	}
}
