package org.joinmastodon.android.fragments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountFeaturedHashtags;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.AccountStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SectionHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class ProfileFeaturedFragment extends BaseStatusListFragment<SearchResult>{
	private Account profileAccount;
	private List<Hashtag> featuredTags;
//	private List<Account> endorsedAccounts;
	private List<Status> pinnedStatuses;
	private boolean tagsLoaded, statusesLoaded;

	public ProfileFeaturedFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		profileAccount=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(SearchResult s){
		ArrayList<StatusDisplayItem> items=switch(s.type){
			case ACCOUNT -> new ArrayList<>(Collections.singletonList(new AccountStatusDisplayItem(s.id, this, s.account)));
			case HASHTAG -> new ArrayList<>(Collections.singletonList(new HashtagStatusDisplayItem(s.id, this, s.hashtag)));
			case STATUS -> StatusDisplayItem.buildItems(this, s.status, accountID, s, knownAccounts, false, true);
		};

		if(s.firstInSection){
			items.add(0, new SectionHeaderStatusDisplayItem(this, getString(switch(s.type){
				case ACCOUNT -> R.string.profile_endorsed_accounts;
				case HASHTAG -> R.string.hashtags;
				case STATUS -> R.string.posts;
			}), getString(R.string.view_all), switch(s.type){
				case ACCOUNT -> (Runnable)this::showAllEndorsedAccounts;
				case HASHTAG -> (Runnable)this::showAllFeaturedHashtags;
				case STATUS -> (Runnable)this::showAllPinnedPosts;
			}));
		}

		return items;
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
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(!statusesLoaded){
			new GetAccountStatuses(profileAccount.id, null, null, 1, GetAccountStatuses.Filter.PINNED)
					 .setCallback(new SimpleCallback<>(this){
						  @Override
						  public void onSuccess(List<Status> result){
							  pinnedStatuses=result;
							  statusesLoaded=true;
							  onOneApiRequestCompleted();
						  }
					 })
					 .exec(accountID);
		}
		if(!tagsLoaded){
			new GetAccountFeaturedHashtags(profileAccount.id)
					 .setCallback(new SimpleCallback<>(this){
						  @Override
						  public void onSuccess(List<Hashtag> result){
							  featuredTags=result;
							  tagsLoaded=true;
							  onOneApiRequestCompleted();
						  }
					 })
					 .exec(accountID);
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	public void onRefresh(){
		statusesLoaded=false;
		tagsLoaded=false;
		super.onRefresh();
	}

	private void onOneApiRequestCompleted(){
		if(tagsLoaded && statusesLoaded){
			ArrayList<SearchResult> results=new ArrayList<>();
			if(!pinnedStatuses.isEmpty()){
				SearchResult res=new SearchResult(pinnedStatuses.get(0));
				res.firstInSection=true;
				results.add(res);
			}
			for(int i=0;i<Math.min(3, featuredTags.size());i++){
				SearchResult res=new SearchResult(featuredTags.get(i));
				res.firstInSection=(i==0);
				results.add(res);
			}
			onDataLoaded(results, false);
		}
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
	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		// no-op
	}

	private void showAllPinnedPosts(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(profileAccount));
		Nav.go(getActivity(), PinnedPostsListFragment.class, args);
	}

	private void showAllFeaturedHashtags(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		ArrayList<Parcelable> tags=featuredTags.stream().map(Parcels::wrap).collect(Collectors.toCollection(ArrayList::new));
		args.putParcelableArrayList("hashtags", tags);
		Nav.go(getActivity(), FeaturedHashtagsListFragment.class, args);
	}

	private void showAllEndorsedAccounts(){

	}
}
