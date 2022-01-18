package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;

import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class ProfileFragment extends StatusListFragment{
	private Account user;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		user=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		setTitle("@"+user.acct);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		String maxID;
		if(offset>0 && !preloadedData.isEmpty())
			maxID=preloadedData.get(preloadedData.size()-1).id;
		else if(offset>0 && !data.isEmpty())
			maxID=data.get(data.size()-1).id;
		else
			maxID=null;
		currentRequest=new GetAccountStatuses(user.id, maxID, null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}
}
