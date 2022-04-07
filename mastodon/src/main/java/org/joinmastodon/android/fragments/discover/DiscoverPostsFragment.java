package org.joinmastodon.android.fragments.discover;

import org.joinmastodon.android.api.requests.trends.GetTrendingStatuses;
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
}
