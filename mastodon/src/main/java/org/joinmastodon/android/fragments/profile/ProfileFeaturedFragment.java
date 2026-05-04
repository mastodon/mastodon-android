package org.joinmastodon.android.fragments.profile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.BatchRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountEndorsements;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.HashtagFeaturedTimelineFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.account_list.FeaturedAccountListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.AccountStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SectionHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class ProfileFeaturedFragment extends BaseStatusListFragment<SearchResult>{
	private Account profileAccount;
	private boolean isSelf;
	private boolean hasMoreFeaturedAccounts;

	public ProfileFeaturedFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		profileAccount=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		isSelf=AccountSessionManager.getInstance().isSelf(accountID, profileAccount);
		setEmptyText(isSelf ? R.string.profile_featured_empty_self : R.string.profile_featured_empty);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(SearchResult s){
		ArrayList<StatusDisplayItem> items=switch(s.type){
			case ACCOUNT -> new ArrayList<>(Collections.singletonList(new AccountStatusDisplayItem(s.id, this, getActivity(), s.account, accountID)));
			case HASHTAG, STATUS -> throw new IllegalStateException();
		};

		if(s.firstInSection){
			items.add(0, new SectionHeaderStatusDisplayItem(this, getActivity(), getString(switch(s.type){
				case ACCOUNT -> R.string.profile_endorsed_accounts;
				case HASHTAG, STATUS -> throw new IllegalStateException();
			}), getString(R.string.view_all), switch(s.type){
				case ACCOUNT -> hasMoreFeaturedAccounts ? (Runnable)this::showAllEndorsedAccounts : null;
				case HASHTAG, STATUS -> throw new IllegalStateException();
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
	protected Status asStatus(SearchResult s){
		return s.type==SearchResult.Type.STATUS ? s.status : null;
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
			case HASHTAG -> {
				Bundle args=new Bundle();
				args.putParcelable("targetAccount", Parcels.wrap(profileAccount));
				args.putParcelable("hashtag", Parcels.wrap(res.hashtag));
				args.putString("account", accountID);
				Nav.go(getActivity(), HashtagFeaturedTimelineFragment.class, args);
			}
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
		// Batch request because this will also load collections when those are ready
		currentRequest=new BatchRequest(Map.of("accounts", new GetAccountEndorsements(profileAccount.id, 5, null)))
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Map<String, Object> result){
						if(getActivity()==null)
							return;
						HeaderPaginationList<Account> accounts=(HeaderPaginationList<Account>) result.get("accounts");
						ArrayList<SearchResult> results=new ArrayList<>();
						for(int i=0;i<accounts.size();i++){
							SearchResult res=new SearchResult(accounts.get(i));
							res.firstInSection=(i==0);
							results.add(res);
						}
						hasMoreFeaturedAccounts=accounts.nextPageUri!=null;
						onDataLoaded(results, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
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
	protected void onRelationshipsLoaded(){
		for(int i=0;i<list.getChildCount();i++){
			if(list.getChildViewHolder(list.getChildAt(i)) instanceof AccountStatusDisplayItem.Holder ah){
				ah.realHolder.bindRelationship();
			}
		}
	}

	@Override
	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		if(holder instanceof FooterStatusDisplayItem.Holder && siblingHolder instanceof StatusDisplayItem.Holder<?> sdi && sdi.getItemID().startsWith("post_")){
			super.drawDivider(child, bottomSibling, holder, siblingHolder, parent, c, paint);
		}
	}

	@Override
	protected void initializeEmptyView(View contentView){
		ViewStub emptyStub=contentView.findViewById(R.id.empty);
		emptyStub.setLayoutResource(R.layout.empty_with_elephant);
		super.initializeEmptyView(contentView);
		TextView emptySecondary=contentView.findViewById(R.id.empty_text_secondary);
		if(isSelf){
			emptySecondary.setText(getString(R.string.profile_featured_empty_self_description));
		}else{
			String text=getString(R.string.profile_featured_empty_description, profileAccount.displayName);
			if(GlobalUserPreferences.customEmojiInNames){
				emptySecondary.setText(HtmlParser.parseCustomEmoji(text, profileAccount.emojis));
				UiUtils.loadCustomEmojiInTextView(emptySecondary);
			}else{
				emptySecondary.setText(text);
			}
		}
	}

	private void showAllEndorsedAccounts(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(profileAccount));
		Nav.go(getActivity(), FeaturedAccountListFragment.class, args);
	}
}
