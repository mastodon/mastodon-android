package org.joinmastodon.android.api.requests.filters;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.LegacyFilter;

import java.util.List;

public class GetLegacyFilters extends MastodonAPIRequest<List<LegacyFilter>>{
	public GetLegacyFilters(){
		super(HttpMethod.GET, "/filters", new TypeToken<>(){});
	}
}
