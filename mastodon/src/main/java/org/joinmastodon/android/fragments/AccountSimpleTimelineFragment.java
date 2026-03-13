package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class AccountSimpleTimelineFragment extends StatusListFragment{
	private Account user;
	private GetAccountStatuses.Filter filter;
	private String maxID;

	public static AccountSimpleTimelineFragment newInstance(String accountID, Account profileAccount, GetAccountStatuses.Filter filter, boolean load){
		AccountSimpleTimelineFragment f=new AccountSimpleTimelineFragment();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(profileAccount));
		args.putInt("filter", filter.ordinal());
		if(!load)
			args.putBoolean("noAutoLoad", true);
		args.putBoolean("__is_tab", true);
		f.setArguments(args);
		return f;
	}

	public AccountSimpleTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	@Override
	public void onAttach(Activity activity){
		user=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		filter=GetAccountStatuses.Filter.values()[getArguments().getInt("filter")];
		super.onAttach(activity);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountStatuses(user.id, offset>0 ? maxID : null, null, count, filter, null)
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

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.setBackground(null); // prevents unnecessary overdraw
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}
}
