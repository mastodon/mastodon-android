package org.joinmastodon.android.fragments.account_list;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountFollowing;
import org.joinmastodon.android.api.requests.accounts.SearchAccounts;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

@SuppressLint("ValidFragment") // This shouldn't be part of any saved states anyway
public class AddNewListMembersFragment extends AccountSearchFragment{
	private Listener listener;
	private String maxID;

	public AddNewListMembersFragment(Listener listener){
		this.listener=listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
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
							onDataLoaded(result.stream().map(a->new AccountViewModel(a, accountID)).collect(Collectors.toList()), result.nextPageUri!=null);
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
			Runnable onDone=()->{
				holder.setActionProgressVisible(false);
				holder.itemView.setHasTransientState(false);
				onBindViewHolder(holder);
			};
			AccountViewModel account=holder.getItem();
			if(listener.isAccountInList(account)){
				listener.removeAccountAccountFromList(account, onDone);
			}else{
				listener.addAccountToList(account, onDone);
			}
		});
	}

	@Override
	protected void onBindViewHolder(AccountViewHolder holder){
		Button button=holder.getButton();
		int textRes, styleRes;
		if(listener.isAccountInList(holder.getItem())){
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

	public interface Listener{
		boolean isAccountInList(AccountViewModel account);
		void addAccountToList(AccountViewModel account, Runnable onDone);
		void removeAccountAccountFromList(AccountViewModel account, Runnable onDone);
	}
}
