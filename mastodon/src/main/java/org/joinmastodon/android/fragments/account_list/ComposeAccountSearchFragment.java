package org.joinmastodon.android.fragments.account_list;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.parceler.Parcels;

import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

public class ComposeAccountSearchFragment extends BaseAccountListFragment{
	private LinearLayout searchLayout;
	private EditText searchEdit;
	private ImageButton clearSearchButton;
	private String currentQuery;
	private Runnable debouncer=()->{
		currentQuery=searchEdit.getText().toString();
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		if(!TextUtils.isEmpty(currentQuery))
			loadData();
	};
	private boolean resultDelivered;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRefreshEnabled(false);
		setEmptyText("");
		dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		searchLayout=new LinearLayout(view.getContext());
		searchLayout.setOrientation(LinearLayout.HORIZONTAL);

		searchEdit=new EditText(view.getContext());
		searchEdit.setHint(R.string.search_hint);
		searchEdit.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		searchEdit.setBackground(null);
		searchEdit.addTextChangedListener(new SimpleTextWatcher(e->{
			searchEdit.removeCallbacks(debouncer);
			searchEdit.postDelayed(debouncer, 300);
		}));
		searchEdit.setImeActionLabel(null, EditorInfo.IME_ACTION_SEARCH);
		searchEdit.setOnEditorActionListener((v, actionId, event)->{
			searchEdit.removeCallbacks(debouncer);
			debouncer.run();
			return true;
		});
		searchLayout.addView(searchEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

		clearSearchButton=new ImageButton(view.getContext());
		clearSearchButton.setImageResource(R.drawable.ic_baseline_close_24);
		clearSearchButton.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnSurfaceVariant)));
		clearSearchButton.setBackground(UiUtils.getThemeDrawable(getToolbarContext(), android.R.attr.actionBarItemBackground));
		clearSearchButton.setOnClickListener(v->searchEdit.setText(""));
		searchLayout.addView(clearSearchButton, new LinearLayout.LayoutParams(V.dp(56), ViewGroup.LayoutParams.MATCH_PARENT));

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
		if(searchLayout.getParent()!=null)
			((ViewGroup) searchLayout.getParent()).removeView(searchLayout);
		getToolbar().addView(searchLayout, new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		getToolbar().setBackgroundResource(R.drawable.bg_m3_surface3);
		searchEdit.requestFocus();
	}

	@Override
	protected boolean wantsElevationOnScrollEffect(){
		return false;
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		super.onConfigureViewHolder(holder);
		holder.setOnClickListener(this::onItemClick);
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
}
