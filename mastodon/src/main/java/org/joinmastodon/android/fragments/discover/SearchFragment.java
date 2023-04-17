package org.joinmastodon.android.fragments.discover;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.AccountStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SearchFragment extends BaseStatusListFragment<SearchResult>{
	private String currentQuery;
	private List<StatusDisplayItem> prevDisplayItems;
	private EnumSet<SearchResult.Type> currentFilter=EnumSet.allOf(SearchResult.Type.class);
	private List<SearchResult> unfilteredResults=Collections.emptyList();
	private HideableSingleViewRecyclerAdapter headerAdapter;
	private ProgressVisibilityListener progressVisibilityListener;
	private InputMethodManager imm;

	private TabLayout tabLayout;

	public SearchFragment(){
		setLayout(R.layout.fragment_search);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
		loadData();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		imm=activity.getSystemService(InputMethodManager.class);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(SearchResult s){
		return switch(s.type){
			case ACCOUNT -> Collections.singletonList(new AccountStatusDisplayItem(s.id, this, s.account));
			case HASHTAG -> Collections.singletonList(new HashtagStatusDisplayItem(s.id, this, s.hashtag));
			case STATUS -> StatusDisplayItem.buildItems(this, s.status, accountID, s, knownAccounts, false, true);
		};
	}

	@Override
	protected void addAccountToKnown(SearchResult s){
		Account acc=switch(s.type){
			case ACCOUNT -> s.account;
			case STATUS -> s.status.account;
			case HASHTAG -> null;
		};
		if(acc!=null && !knownAccounts.containsKey(acc.id))
			knownAccounts.put(acc.id, acc);
	}

	@Override
	public void onItemClick(String id){
		SearchResult res=getResultByID(id);
		if(res==null)
			return;
		switch(res.type){
			case ACCOUNT -> {
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("profileAccount", Parcels.wrap(res.account));
				Nav.go(getActivity(), ProfileFragment.class, args);
			}
			case HASHTAG -> UiUtils.openHashtagTimeline(getActivity(), accountID, res.hashtag.name);
			case STATUS -> {
				Status status=res.status.getContentStatus();
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("status", Parcels.wrap(status));
				if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
					args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
				Nav.go(getActivity(), ThreadFragment.class, args);
			}
		}
		if(res.type!=SearchResult.Type.STATUS)
			AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putRecentSearch(res);
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(isInRecentMode()){
			AccountSessionManager.getInstance().getAccount(accountID).getCacheController().getRecentSearches(sr->{
				if(getActivity()==null)
					return;
				unfilteredResults=sr;
				prevDisplayItems=new ArrayList<>(displayItems);
				onDataLoaded(sr, false);
			});
		}else{
			progressVisibilityListener.onProgressVisibilityChanged(true);
			currentRequest=new GetSearchResults(currentQuery, null, true)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(SearchResults result){
							ArrayList<SearchResult> results=new ArrayList<>();
							if(result.accounts!=null){
								for(Account acc:result.accounts)
									results.add(new SearchResult(acc));
							}
							if(result.hashtags!=null){
								for(Hashtag tag:result.hashtags)
									results.add(new SearchResult(tag));
							}
							if(result.statuses!=null){
								for(Status status:result.statuses)
									results.add(new SearchResult(status));
							}
							prevDisplayItems=new ArrayList<>(displayItems);
							unfilteredResults=results;
							onDataLoaded(filterSearchResults(results), false);
						}

						@Override
						public void onError(ErrorResponse error){
							currentRequest=null;
							Activity a=getActivity();
							if(a==null)
								return;
							error.showToast(a);
							if(progressVisibilityListener!=null)
								progressVisibilityListener.onProgressVisibilityChanged(false);
						}
					})
					.exec(accountID);
		}
	}

	@Override
	public void updateList(){
		if(prevDisplayItems==null){
			super.updateList();
			return;
		}
		UiUtils.updateList(prevDisplayItems, displayItems, list, adapter, (i1, i2)->i1.parentID.equals(i2.parentID) && i1.index==i2.index && i1.getType()==i2.getType());
		boolean recent=isInRecentMode();
		if(recent!=headerAdapter.isVisible())
			headerAdapter.setVisible(recent);
		imgLoader.forceUpdateImages();
		prevDisplayItems=null;
	}

	@Override
	protected void onDataLoaded(List<SearchResult> d, boolean more){
		super.onDataLoaded(d, more);
		if(progressVisibilityListener!=null)
			progressVisibilityListener.onProgressVisibilityChanged(false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		tabLayout=view.findViewById(R.id.tabbar);
		tabLayout.setTabTextSize(V.dp(16));
		tabLayout.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorTabInactive), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		tabLayout.addTab(tabLayout.newTab().setText(R.string.search_all));
		tabLayout.addTab(tabLayout.newTab().setText(R.string.search_people));
		tabLayout.addTab(tabLayout.newTab().setText(R.string.hashtags));
		tabLayout.addTab(tabLayout.newTab().setText(R.string.posts));
		for(int i=0;i<tabLayout.getTabCount();i++){
			tabLayout.getTabAt(i).view.textView.setAllCaps(true);
		}
		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){
				setFilter(switch(tab.getPosition()){
					case 0 -> EnumSet.allOf(SearchResult.Type.class);
					case 1 -> EnumSet.of(SearchResult.Type.ACCOUNT);
					case 2 -> EnumSet.of(SearchResult.Type.HASHTAG);
					case 3 -> EnumSet.of(SearchResult.Type.STATUS);
					default -> throw new IllegalStateException("Unexpected value: "+tab.getPosition());
				});
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				scrollToTop();
			}
		});
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View header=getActivity().getLayoutInflater().inflate(R.layout.item_recent_searches_header, list, false);
		header.findViewById(R.id.clear).setOnClickListener(this::onClearRecentClick);
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(headerAdapter=new HideableSingleViewRecyclerAdapter(header));
		adapter.addAdapter(super.getAdapter());
		headerAdapter.setVisible(isInRecentMode());
		return adapter;
	}

	public void setQuery(String q){
		if(Objects.equals(q, currentQuery) || q.isBlank())
			return;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		currentQuery=q;
		refreshing=true;
		doLoadData(0, 0);
	}

	private void setFilter(EnumSet<SearchResult.Type> filter){
		if(filter.equals(currentFilter))
			return;
		currentFilter=filter;
		if(isInRecentMode())
			return;
		// This can be optimized by not rebuilding display items every time filter is changed, but I'm too lazy
		prevDisplayItems=new ArrayList<>(displayItems);
		refreshing=true;
		onDataLoaded(filterSearchResults(unfilteredResults), false);
	}

	private List<SearchResult> filterSearchResults(List<SearchResult> input){
		if(currentFilter.size()==SearchResult.Type.values().length)
			return input;
		return input.stream().filter(sr->currentFilter.contains(sr.type)).collect(Collectors.toList());
	}

	protected SearchResult getResultByID(String id){
		for(SearchResult s:data){
			if(s.id.equals(id)){
				return s;
			}
		}
		return null;
	}

	private void onClearRecentClick(View v){
		AccountSessionManager.getInstance().getAccount(accountID).getCacheController().clearRecentSearches();
		if(isInRecentMode()){
			prevDisplayItems=new ArrayList<>(displayItems);
			refreshing=true;
			onDataLoaded(unfilteredResults=Collections.emptyList(), false);
		}
	}

	private boolean isInRecentMode(){
		return TextUtils.isEmpty(currentQuery);
	}

	public void setProgressVisibilityListener(ProgressVisibilityListener progressVisibilityListener){
		this.progressVisibilityListener=progressVisibilityListener;
	}

	@Override
	public void onScrollStarted(){
		super.onScrollStarted();
		if(imm.isActive()){
			imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
		}
	}

	@FunctionalInterface
	public interface ProgressVisibilityListener{
		void onProgressVisibilityChanged(boolean visible);
	}
}
