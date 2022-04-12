package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Filter;

import java.util.List;

public class GetWordFilters extends MastodonAPIRequest<List<Filter>>{
	public GetWordFilters(){
		super(HttpMethod.GET, "/filters", new TypeToken<>(){});
	}
}
