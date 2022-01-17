package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class HomeTimelineFragment extends StatusListFragment{
	private String accountID;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.app_name);
		accountID=getArguments().getString("account");
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetHomeTimeline(null, null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}
}
