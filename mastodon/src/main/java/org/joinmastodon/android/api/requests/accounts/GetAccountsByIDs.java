package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

import java.util.Collection;
import java.util.List;

public class GetAccountsByIDs extends MastodonAPIRequest<List<Account>>{
	public GetAccountsByIDs(Collection<String> ids){
		super(HttpMethod.GET, "/accounts", new TypeToken<>(){});
		for(String id:ids)
			addQueryParameter("id[]", id);
	}
}
