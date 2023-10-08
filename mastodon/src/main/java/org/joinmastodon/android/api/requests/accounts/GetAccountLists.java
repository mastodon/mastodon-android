package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowList;

import java.util.List;

public class GetAccountLists extends MastodonAPIRequest<List<FollowList>>{
	public GetAccountLists(String id){
		super(HttpMethod.GET, "/accounts/"+id+"/lists", new TypeToken<>(){});
	}
}
