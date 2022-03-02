package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.api.requests.timelines.GetHashtagTimeline;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class HashtagTimelineFragment extends StatusListFragment{
	private String hashtag;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		hashtag=getArguments().getString("hashtag");
		setTitle('#'+hashtag);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetHashtagTimeline(hashtag, offset==0 ? null : getMaxID(), null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}
}
