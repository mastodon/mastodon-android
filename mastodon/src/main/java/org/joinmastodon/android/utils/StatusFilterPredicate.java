package org.joinmastodon.android.utils;

import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Status;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StatusFilterPredicate implements Predicate<Status>{
	private final List<Filter> filters;

	public StatusFilterPredicate(List<Filter> filters){
		this.filters=filters;
	}

	public StatusFilterPredicate(String accountID, Filter.FilterContext context){
		filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(context)).collect(Collectors.toList());
	}

	@Override
	public boolean test(Status status){
		for(Filter filter:filters){
			if(filter.matches(status))
				return false;
		}
		return true;
	}
}
