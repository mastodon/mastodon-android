package org.joinmastodon.android.api.requests.lists;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowList;

import java.util.List;

public class GetLists extends MastodonAPIRequest<List<FollowList>>{
	public GetLists(){
		super(HttpMethod.GET, "/lists", new TypeToken<>(){});
	}
}
