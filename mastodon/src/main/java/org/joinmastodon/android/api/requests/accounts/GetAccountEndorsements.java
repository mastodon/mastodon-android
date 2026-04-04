package org.joinmastodon.android.api.requests.accounts;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.Account;

public class GetAccountEndorsements extends HeaderPaginationRequest<Account>{
	public GetAccountEndorsements(String accountID, int limit, String maxID){
		super(HttpMethod.GET, "/accounts/"+accountID+"/endorsements", new TypeToken<>(){});
		addQueryParameter("limit", limit+"");
		if(!TextUtils.isEmpty(maxID))
			addQueryParameter("max_id", maxID);
	}
}
