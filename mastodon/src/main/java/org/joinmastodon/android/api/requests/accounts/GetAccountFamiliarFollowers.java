package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FamiliarFollowers;

import java.util.Collection;
import java.util.List;

public class GetAccountFamiliarFollowers extends MastodonAPIRequest<List<FamiliarFollowers>>{
	public GetAccountFamiliarFollowers(Collection<String> ids){
		super(HttpMethod.GET, "/accounts/familiar_followers", new TypeToken<>(){});
		for(String id:ids){
			addQueryParameter("id[]", id);
		}
	}
}
