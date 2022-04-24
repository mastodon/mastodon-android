package org.joinmastodon.android.fragments.discover;

import org.joinmastodon.android.api.requests.timelines.GetPublicTimeline;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class LocalTimelineFragment extends StatusListFragment{
	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetPublicTimeline(true, false, refreshing ? null : getMaxID(), count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}
}
