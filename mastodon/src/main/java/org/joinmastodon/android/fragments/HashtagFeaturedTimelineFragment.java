package org.joinmastodon.android.fragments;

import android.os.Bundle;

import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

// The difference between this and HashtagTimelineFragment is that this opens from the featured hashtags
// and only shows posts by that account.
public class HashtagFeaturedTimelineFragment extends StatusListFragment{
	private Account targetAccount;
	private Hashtag hashtag;
	private String maxID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		targetAccount=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		hashtag=Parcels.unwrap(getArguments().getParcelable("hashtag"));
		setTitle("#"+hashtag.name);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountStatuses(targetAccount.id, offset>0 ? maxID : null, null, count, GetAccountStatuses.Filter.DEFAULT, hashtag.name)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Status> result){
						if(getActivity()==null)
							return;
						AccountSessionManager.get(accountID).filterStatuses(result, FilterContext.ACCOUNT);
						maxID=result.getNextPageMaxID();
						onDataLoaded(result, result.nextPageUri!=null);
					}
				})
				.exec(accountID);
	}
}
