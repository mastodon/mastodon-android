package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

import java.util.Map;

public class SetAccountPersonalNote extends MastodonAPIRequest<Relationship>{
	public SetAccountPersonalNote(String id, String note){
		super(HttpMethod.POST, "/accounts/"+id+"/note", Relationship.class);
		setRequestBody(Map.of("comment", note));
	}
}
