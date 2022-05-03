package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.requests.statuses.GetStatusFavorites;
import org.joinmastodon.android.model.Account;

public class StatusFavoritesListFragment extends StatusRelatedAccountListFragment{
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(getResources().getQuantityString(R.plurals.x_favorites, status.favouritesCount, status.favouritesCount));
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count){
		return new GetStatusFavorites(status.id, maxID, count);
	}
}
