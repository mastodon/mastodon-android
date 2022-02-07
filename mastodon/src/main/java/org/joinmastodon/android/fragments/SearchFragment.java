package org.joinmastodon.android.fragments;

import org.joinmastodon.android.api.requests.trends.GetTrendingStatuses;
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
}
