package org.joinmastodon.android.fragments.collections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.collections.GetAccountCollections;
import org.joinmastodon.android.api.requests.collections.GetCollectionsFeaturingAccount;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.collections.AccountCollections;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;

import java.util.List;

public class OwnCollectionListFragment extends CollectionListFragment{
	private View ownTab, featuringTab;
	private boolean showOwn=true;
	private TextView emptySecondary;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		profileAccount=AccountSessionManager.get(accountID).self;
		setLayout(R.layout.fragment_own_collections);

		setTitle(R.string.profile_collections);
		setEmptyButtonText(R.string.create_collection);
		loadData();
	}

	@Override
	protected MastodonAPIRequest<AccountCollections> getRequest(int offset, int count){
		return showOwn ? new GetAccountCollections(profileAccount.id, offset, count) : new GetCollectionsFeaturingAccount(profileAccount.id, offset, count);
	}

	@Override
	protected void onDataLoaded(List<CollectionViewModel> d, boolean more){
		updateEmptyState();
		super.onDataLoaded(d, more);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=super.onCreateView(inflater, container, savedInstanceState);

		ownTab=view.findViewById(R.id.collections_own_tab);
		featuringTab=view.findViewById(R.id.collections_featuring_tab);
		ownTab.setOnClickListener(this::onTabClick);
		featuringTab.setOnClickListener(this::onTabClick);
		(showOwn ? ownTab : featuringTab).setSelected(true);

		OutlineProviders.clipToRoundRect(view.findViewById(R.id.tabbar_inner), 20);

		NestedRecyclerScrollView scroller=view.findViewById(R.id.scroller);
		scroller.setScrollableChildSupplier(()->list);
		scroller.setTakePriorityOverChildViews(true);

		updateEmptyState();

		return view;
	}

	@Override
	protected void initializeEmptyView(View contentView){
		ViewStub emptyStub=contentView.findViewById(R.id.empty);
		emptyStub.setLayoutResource(R.layout.empty_with_elephant);
		super.initializeEmptyView(contentView);
		emptySecondary=contentView.findViewById(R.id.empty_text_secondary);
		emptyButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_phosphor_plus_light, 0, 0, 0);
	}

	@Override
	protected void onEmptyViewBtnClick(){
		// TODO
	}

	private void updateEmptyState(){
		setEmptyText(showOwn ? R.string.own_collections_empty : R.string.featuring_collections_empty);
		emptySecondary.setText(showOwn ? R.string.own_collections_empty_description : R.string.featuring_collections_empty_description);
		setEmptyButtonVisible(showOwn);
	}

	private void onTabClick(View v){
		boolean newOwn=v.getId()==R.id.collections_own_tab;
		if(newOwn==showOwn)
			return;
		showOwn=newOwn;
		ownTab.setSelected(showOwn);
		featuringTab.setSelected(!showOwn);
		showProgress();
		refreshing=true;
		loadData();
	}
}
