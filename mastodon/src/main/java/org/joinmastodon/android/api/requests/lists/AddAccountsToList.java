package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import okhttp3.FormBody;

public class AddAccountsToList extends ResultlessMastodonAPIRequest{
	public AddAccountsToList(String listID, Collection<String> accountIDs){
		super(HttpMethod.POST, "/lists/"+listID+"/accounts");
		FormBody.Builder builder=new FormBody.Builder(StandardCharsets.UTF_8);
		for(String id:accountIDs){
			builder.add("account_ids[]", id);
		}
		setRequestBody(builder.build());
	}
}
