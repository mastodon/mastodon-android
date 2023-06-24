package org.joinmastodon.android.api.requests.filters;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterKeyword;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpdateFilter extends MastodonAPIRequest<Filter>{
	public UpdateFilter(String id, String title, EnumSet<FilterContext> context, FilterAction action, int expiresIn, List<FilterKeyword> words, List<String> deletedWords){
		super(HttpMethod.PUT, "/filters/"+id, Filter.class);

		List<KeywordAttribute> attrs=Stream.of(
				words.stream().map(w->new KeywordAttribute(w.id, null, w.keyword, w.wholeWord)),
				deletedWords.stream().map(wid->new KeywordAttribute(wid, true, null, null))
		).flatMap(Function.identity()).collect(Collectors.toList());
		setRequestBody(new FilterRequest(title, context, action, expiresIn==0 ? null : expiresIn, attrs));
	}

	@Override
	protected String getPathPrefix(){
		return "/api/v2";
	}
}
