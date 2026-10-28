package org.joinmastodon.android.fragments.account_list;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.ResultlessMastodonAPIRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountFollowing;
import org.joinmastodon.android.api.requests.accounts.SearchAccounts;
import org.joinmastodon.android.api.requests.lists.AddAccountsToList;
import org.joinmastodon.android.api.requests.lists.RemoveAccountsFromList;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.AccountAddedToListEvent;
import org.joinmastodon.android.events.AccountRemovedFromListEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

public class AddNewListMembersFragment extends AccountSearchFragment{
	private String maxID;
	private HashSet<String> accountIDsInList=new HashSet<>();
	private FollowList followList;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountIDsInList.addAll((savedInstanceState==null ? getArguments() : savedInstanceState).getStringArrayList("accountIDs"));
		followList=Parcels.unwrap(getArguments().getParcelable("list"));
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(TextUtils.isEmpty(currentQuery)){
			currentRequest=new GetAccountFollowing(AccountSessionManager.get(accountID).self.id, offset>0 ? maxID : null, count)
					.setCallback(new SimpleCallback<>(this){
						@Override
						public void onSuccess(HeaderPaginationList<Account> result){
							setEmptyText("");
							onDataLoaded(result.stream().map(a->new AccountViewModel(a, accountID, getActivity())).collect(Collectors.toList()), result.nextPageUri!=null);
							maxID=result.getNextPageMaxID();
						}
					})
					.exec(accountID);
		}else{
			refreshing=true;
			currentRequest=new SearchAccounts(currentQuery, 0, 0, false, true)
					.setCallback(new SimpleCallback<>(this){
						@Override
						public void onSuccess(List<Account> result){
							AddNewListMembersFragment.this.onSuccess(result);
						}
					})
					.exec(accountID);
		}
	}

	@Override
	protected String getSearchViewPlaceholder(){
		return getString(R.string.search_among_people_you_follow);
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		holder.setStyle(AccountViewHolder.AccessoryType.CUSTOM_BUTTON, false);
		holder.setOnLongClickListener(vh->false);
		Button button=holder.getButton();
		button.setPadding(V.dp(24), 0, V.dp(24), 0);
		button.setMinimumWidth(0);
		button.setMinWidth(0);
		button.setOnClickListener(v->{
			holder.setActionProgressVisible(true);
			holder.itemView.setHasTransientState(true);
			AccountViewModel account=holder.getItem();
			ResultlessMastodonAPIRequest req;
			boolean add=!accountIDsInList.contains(account.account.id);
			if(add){
				req=new AddAccountsToList(followList.id, Set.of(account.account.id));
			}else{
				req=new RemoveAccountsFromList(followList.id, Set.of(account.account.id));
			}

			req.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Void result){
							holder.setActionProgressVisible(false);
							holder.itemView.setHasTransientState(false);
							if(add){
								accountIDsInList.add(account.account.id);
								E.post(new AccountAddedToListEvent(accountID, followList.id, account.account));
							}else{
								accountIDsInList.remove(account.account.id);
								E.post(new AccountRemovedFromListEvent(accountID, followList.id, account.account.id));
							}
							onBindViewHolder(holder);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
							holder.setActionProgressVisible(false);
							holder.itemView.setHasTransientState(false);
							onBindViewHolder(holder);
						}
					})
					.exec(accountID);
		});
	}

	@Override
	protected void onBindViewHolder(AccountViewHolder holder){
		Button button=holder.getButton();
		int textRes, styleRes;
		if(accountIDsInList.contains(holder.getItem().account.id)){
			textRes=R.string.remove;
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal_Error;
		}else{
			textRes=R.string.add;
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}
		button.setText(textRes);
		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	@Override
	protected void loadRelationships(List<AccountViewModel> accounts){
		// no-op
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("accountIDs", new ArrayList<>(accountIDsInList));
	}
}
