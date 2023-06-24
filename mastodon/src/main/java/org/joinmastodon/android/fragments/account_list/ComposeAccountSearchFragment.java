package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.SearchViewHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.parceler.Parcels;

import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class ComposeAccountSearchFragment extends BaseAccountListFragment{
	private String currentQuery;
	private boolean resultDelivered;
	private SearchViewHelper searchViewHelper;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRefreshEnabled(false);
		setEmptyText("");
		dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		searchViewHelper=new SearchViewHelper(getActivity(), getToolbarContext(), getString(R.string.search_hint));
		searchViewHelper.setListeners(this::onQueryChanged, null);
		searchViewHelper.addDivider(contentView);
		super.onViewCreated(view, savedInstanceState);

		view.setBackgroundResource(R.drawable.bg_m3_surface3);
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Surface, R.attr.colorM3Primary, 0.11f);
		setStatusBarColor(color);
		setNavigationBarColor(color);
	}

	@Override
	protected void doLoadData(int offset, int count){
		refreshing=true;
		currentRequest=new GetSearchResults(currentQuery, GetSearchResults.Type.ACCOUNTS, false)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(SearchResults result){
						setEmptyText(R.string.no_search_results);
						onDataLoaded(result.accounts.stream().map(a->new AccountViewModel(a, accountID)).collect(Collectors.toList()), false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		searchViewHelper.install(getToolbar());
	}

	@Override
	protected boolean wantsElevationOnScrollEffect(){
		return false;
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		super.onConfigureViewHolder(holder);
		holder.setOnClickListener(this::onItemClick);
		holder.setStyle(AccountViewHolder.AccessoryType.NONE, false);
	}

	private void onItemClick(AccountViewHolder holder){
		if(resultDelivered)
			return;

		resultDelivered=true;
		Bundle res=new Bundle();
		res.putParcelable("selectedAccount", Parcels.wrap(holder.getItem().account));
		setResult(true, res);
		Nav.finish(this, false);
	}

	private void onQueryChanged(String q){
		currentQuery=q;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		if(!TextUtils.isEmpty(currentQuery))
			loadData();
	}
}
