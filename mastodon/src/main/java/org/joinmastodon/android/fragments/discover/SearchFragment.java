package org.joinmastodon.android.fragments.discover;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
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
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class SearchFragment extends BaseStatusListFragment<SearchResult>{
	private String currentQuery;
	private List<StatusDisplayItem> prevDisplayItems;
	private EnumSet<SearchResult.Type> currentFilter=EnumSet.allOf(SearchResult.Type.class);
	private List<SearchResult> unfilteredResults=Collections.emptyList();
	private InputMethodManager imm;

	public SearchFragment(){
		setLayout(R.layout.fragment_search);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
		setEmptyText(R.string.no_search_results);
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
		GetSearchResults.Type type;
		if(currentFilter.size()==1){
			type=switch(currentFilter.iterator().next()){
				case ACCOUNT -> GetSearchResults.Type.ACCOUNTS;
				case HASHTAG -> GetSearchResults.Type.HASHTAGS;
				case STATUS -> GetSearchResults.Type.STATUSES;
			};
		}else{
			type=null;
		}
		if(currentQuery==null)
			return;
		currentRequest=new GetSearchResults(currentQuery, type, true)
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
					}
				})
				.exec(accountID);
	}

	@Override
	public void updateList(){
		if(prevDisplayItems==null){
			super.updateList();
			return;
		}
		UiUtils.updateList(prevDisplayItems, displayItems, list, adapter, (i1, i2)->i1.parentID.equals(i2.parentID) && i1.index==i2.index && i1.getType()==i2.getType());
		imgLoader.forceUpdateImages();
		prevDisplayItems=null;
	}

	public void setQuery(String q, SearchResult.Type filter){
		if(q.isBlank())
			return;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		currentQuery=q;
		if(filter==null)
			currentFilter=EnumSet.allOf(SearchResult.Type.class);
		else
			currentFilter=EnumSet.of(filter);
		refreshing=true;
		loadData();
	}

	private void setFilter(EnumSet<SearchResult.Type> filter){
		if(filter.equals(currentFilter))
			return;
		currentFilter=filter;
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
