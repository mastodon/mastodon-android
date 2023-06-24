package org.joinmastodon.android.api.requests.filters;

import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;

import java.util.EnumSet;
import java.util.List;

class FilterRequest{
	public String title;
	public EnumSet<FilterContext> context;
	public FilterAction filterAction;
	public Integer expiresIn;
	public List<KeywordAttribute> keywordsAttributes;

	public FilterRequest(String title, EnumSet<FilterContext> context, FilterAction filterAction, Integer expiresIn, List<KeywordAttribute> keywordsAttributes){
		this.title=title;
		this.context=context;
		this.filterAction=filterAction;
		this.expiresIn=expiresIn;
		this.keywordsAttributes=keywordsAttributes;
	}
}
