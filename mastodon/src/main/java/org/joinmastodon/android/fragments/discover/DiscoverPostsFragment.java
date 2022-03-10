package org.joinmastodon.android.fragments.discover;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.api.requests.trends.GetTrendingStatuses;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class DiscoverPostsFragment extends StatusListFragment{
	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingStatuses(count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, false);
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
