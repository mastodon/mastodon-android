package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountFollowers;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

public class FollowerListFragment extends AccountRelatedAccountListFragment{

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		final boolean isSelf = AccountSessionManager.getInstance().isSelf(accountID, account);
		final int followersCountResId = isSelf ? R.plurals.my_x_followers : R.plurals.x_followers;
		setSubtitle(getResources().getQuantityString(followersCountResId, (int)(account.followersCount%1000), account.followersCount));
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count){
		return new GetAccountFollowers(account.id, maxID, count);
	}
}
