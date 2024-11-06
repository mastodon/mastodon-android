package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountFamiliarFollowers;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FamiliarFollowers;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.parceler.Parcels;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class FamiliarFollowerListFragment extends BaseAccountListFragment{
	protected Account account;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		account=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		setTitle("@"+account.acct);
		int count=getArguments().getInt("count");
		setSubtitle(getResources().getQuantityString(R.plurals.x_followers_you_know, count, count));
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountFamiliarFollowers(Set.of(account.id))
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FamiliarFollowers> result){
						onDataLoaded(result.get(0).accounts.stream().map(a->new AccountViewModel(a, accountID, getActivity())).collect(Collectors.toList()), false);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onResume(){
		super.onResume();
		if(!loaded && !dataLoading)
			loadData();
	}
}
