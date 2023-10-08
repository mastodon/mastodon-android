package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

import java.util.List;

public class SearchAccounts extends MastodonAPIRequest<List<Account>>{
	public SearchAccounts(String q, int limit, int offset, boolean resolve, boolean following){
		super(HttpMethod.GET, "/accounts/search", new TypeToken<>(){});
		addQueryParameter("q", q);
		if(limit>0)
			addQueryParameter("limit", limit+"");
		if(offset>0)
			addQueryParameter("offset", offset+"");
		if(resolve)
			addQueryParameter("resolve", "true");
		if(following)
			addQueryParameter("following", "true");
	}
}
