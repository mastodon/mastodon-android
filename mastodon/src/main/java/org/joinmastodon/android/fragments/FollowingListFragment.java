package org.joinmastodon.android.fragments;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountFollowing;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.parceler.Parcels;

import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class FollowingListFragment extends BaseAccountListFragment{
	private Account account;
	private String nextMaxID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		account=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		setTitle("@"+account.acct);
		setSubtitle(getResources().getQuantityString(R.plurals.x_following, account.followingCount, account.followingCount));
	}

	@Override
	public void onResume(){
		super.onResume();
		if(!loaded && !dataLoading)
			loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountFollowing(account.id, offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						onDataLoaded(result.stream().map(AccountItem::new).collect(Collectors.toList()), nextMaxID!=null);
					}
				})
				.exec(accountID);
	}
}
