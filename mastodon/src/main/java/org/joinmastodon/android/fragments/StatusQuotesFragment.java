package org.joinmastodon.android.fragments;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusQuotes;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import me.grishka.appkit.api.SimpleCallback;

public class StatusQuotesFragment extends StatusListFragment{
	private Status status;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		status=Parcels.unwrap(getArguments().getParcelable("status"));
		setTitle(getResources().getQuantityString(R.plurals.x_quotes, (int)status.quotesCount, status.quotesCount));
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetStatusQuotes(status.id, offset>0 ? getMaxID() : null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Status> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result, result.nextPageUri!=null);
					}
				})
				.exec(accountID);
	}
}
