package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetBookmarkedStatuses;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;

import me.grishka.appkit.api.SimpleCallback;

public class BookmarkedStatusListFragment extends StatusListFragment{
	private String nextMaxID;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.bookmarks);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetBookmarkedStatuses(offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Status> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						onDataLoaded(result, nextMaxID!=null);
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		// no-op
	}
}
