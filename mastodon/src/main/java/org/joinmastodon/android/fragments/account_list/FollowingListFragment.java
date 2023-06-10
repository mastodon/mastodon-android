package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountFollowing;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

public class FollowingListFragment extends AccountRelatedAccountListFragment{

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		final boolean isSelf = AccountSessionManager.getInstance().isSelf(accountID, account);
		final int followingCountResId = isSelf ? R.plurals.my_x_following : R.plurals.x_following;
		setSubtitle(getResources().getQuantityString(followingCountResId, (int)(account.followingCount%1000), account.followingCount));
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count){
		return new GetAccountFollowing(account.id, maxID, count);
	}
}
