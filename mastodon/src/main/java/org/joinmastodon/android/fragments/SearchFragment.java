package org.joinmastodon.android.fragments;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.api.requests.trends.GetTrendingStatuses;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class SearchFragment extends StatusListFragment{
	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingStatuses(offset>0 ? getMaxID() : null, null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				}).exec(accountID);
	}

	@Override
	@Subscribe
	public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
		super.onStatusCountersUpdated(ev);
	}

	@Override
	@Subscribe
	public void onStatusDeleted(StatusDeletedEvent ev){
		super.onStatusDeleted(ev);
	}
}
