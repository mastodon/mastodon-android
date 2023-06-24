package org.joinmastodon.android.api.requests.filters;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterKeyword;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class CreateFilter extends MastodonAPIRequest<Filter>{
	public CreateFilter(String title, EnumSet<FilterContext> context, FilterAction action, int expiresIn, List<FilterKeyword> words){
		super(HttpMethod.POST, "/filters", Filter.class);
		setRequestBody(new FilterRequest(title, context, action, expiresIn==0 ? null : expiresIn, words.stream().map(w->new KeywordAttribute(null, null, w.keyword, w.wholeWord)).collect(Collectors.toList())));
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}
}
