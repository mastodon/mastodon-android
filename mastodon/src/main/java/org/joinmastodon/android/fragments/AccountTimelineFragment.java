package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class AccountTimelineFragment extends StatusListFragment{
	private Account user;
	private GetAccountStatuses.Filter filter;

	public AccountTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	public static AccountTimelineFragment newInstance(String accountID, Account profileAccount, GetAccountStatuses.Filter filter, boolean load){
		AccountTimelineFragment f=new AccountTimelineFragment();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(profileAccount));
		args.putString("filter", filter.toString());
		if(!load)
			args.putBoolean("noAutoLoad", true);
		args.putBoolean("__is_tab", true);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		user=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		filter=GetAccountStatuses.Filter.valueOf(getArguments().getString("filter"));
		if(!getArguments().getBoolean("noAutoLoad"))
			loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountStatuses(user.id, offset>0 ? getMaxID() : null, null, count, filter)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
	}
}
