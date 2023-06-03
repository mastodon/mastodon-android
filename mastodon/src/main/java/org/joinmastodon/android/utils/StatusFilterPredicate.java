package org.joinmastodon.android.utils;

import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Status;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StatusFilterPredicate implements Predicate<Status>{
	private final List<LegacyFilter> filters;

	public StatusFilterPredicate(List<LegacyFilter> filters){
		this.filters=filters;
	}

	public StatusFilterPredicate(String accountID, FilterContext context){
		filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(context)).collect(Collectors.toList());
	}

	@Override
	public boolean test(Status status){
		for(LegacyFilter filter:filters){
			if(filter.matches(status))
				return false;
		}
		return true;
	}
}
