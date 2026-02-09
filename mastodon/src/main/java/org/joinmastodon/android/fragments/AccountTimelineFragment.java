package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountFeaturedHashtags;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.CheckablePopupMenu;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableTextView;
import org.joinmastodon.android.ui.views.ExpandableWrappingLinearLayout;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class AccountTimelineFragment extends StatusListFragment{
	private Account user;
	private GetAccountStatuses.Filter filter;
	private TextView headerFilterTitle;
	private CheckablePopupMenu filterMenu;
	private ExpandableWrappingLinearLayout hashtagsView;
	private View expandHashtagsButton;
	private boolean showReplies=false, showBoosts=true;
	private APIRequest<?> featuredHashtagsRequest;
	private List<Hashtag> hashtags=List.of();
	private String hashtagFilter=null;
	private boolean hashtagsLoaded;

	public AccountTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	public static AccountTimelineFragment newInstance(String accountID, Account profileAccount, boolean load){
		AccountTimelineFragment f=new AccountTimelineFragment();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(profileAccount));
		if(!load)
			args.putBoolean("noAutoLoad", true);
		args.putBoolean("__is_tab", true);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Activity activity){
		user=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		filter=GetAccountStatuses.Filter.DEFAULT;
		super.onAttach(activity);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(featuredHashtagsRequest!=null)
			featuredHashtagsRequest.cancel();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountStatuses(user.id, offset>0 ? getMaxID() : null, null, count, filter, hashtagFilter)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						if(getActivity()==null)
							return;
						boolean empty=result.isEmpty();
						AccountSessionManager.get(accountID).filterStatuses(result, FilterContext.ACCOUNT);
						onDataLoaded(result, !empty);
					}
				})
				.exec(accountID);
		if(offset==0 && (refreshing || !hashtagsLoaded)){
			featuredHashtagsRequest=new GetAccountFeaturedHashtags(user.id)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(List<Hashtag> result){
							featuredHashtagsRequest=null;
							hashtags=result;
							updateHashtags();
							hashtagsLoaded=true;
						}

						@Override
						public void onError(ErrorResponse error){
							featuredHashtagsRequest=null;
						}
					})
					.exec(accountID);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.setBackground(null); // prevents unnecessary overdraw
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	private void updateHashtags(){
		if(hashtags.isEmpty()){
			hashtagsView.setVisibility(View.GONE);
		}else{
			hashtagsView.setVisibility(View.VISIBLE);
			hashtagsView.removeAllViews();
			for(Hashtag tag:hashtags){
				CheckableTextView btn=makeHashtagView(tag.name);
				btn.setTag(tag.name);
				btn.setOnClickListener(this::onHashtagClick);
				btn.setChecked(Objects.equals(hashtagFilter, tag.name));
				hashtagsView.addView(btn);
			}
			hashtagsView.addView(expandHashtagsButton);
		}
	}

	protected void onStatusCreated(Status status){
		if(!AccountSessionManager.getInstance().isSelf(accountID, user) || !AccountSessionManager.getInstance().isSelf(accountID, status.account))
			return;
		if(filter==GetAccountStatuses.Filter.DEFAULT){
			// Keep replies to self, discard all other replies
			if(status.inReplyToAccountId!=null && !status.inReplyToAccountId.equals(AccountSessionManager.getInstance().getAccount(accountID).self.id) && !showReplies)
				return;
		}else if(filter==GetAccountStatuses.Filter.MEDIA){
			if(status.mediaAttachments.isEmpty())
				return;
		}
		prependItems(Collections.singletonList(status), true);
	}

	@Override
	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		// no-op
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View header=LayoutInflater.from(getActivity()).inflate(R.layout.header_account_timeline, list, false);
		headerFilterTitle=header.findViewById(R.id.filter_title);
		ImageButton btn=header.findViewById(R.id.filter_button);
		headerFilterTitle.setText(R.string.timeline_filter_posts_and_boosts);
		filterMenu=new CheckablePopupMenu(getActivity())
				.addItem(R.string.timeline_filter_show_replies, false)
				.addItem(R.string.timeline_filter_show_boosts, true)
				.setListener(this::onFilterChanged);
		btn.setOnClickListener(v->{
			filterMenu.show(v);
		});
		hashtagsView=header.findViewById(R.id.hashtags);
		expandHashtagsButton=makeHashtagView("");
		expandHashtagsButton.setId(R.id.expand_button);
		expandHashtagsButton.setOnClickListener(v->hashtagsView.expand());
		hashtagsView.addView(expandHashtagsButton);
		updateHashtags();

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(header));
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@SuppressLint("SetTextI18n")
	private CheckableTextView makeHashtagView(String hashtag){
		CheckableTextView v=new CheckableTextView(getActivity());
		v.setTextAppearance(R.style.m3_label_large);
		v.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
		v.setPadding(V.dp(12), 0, V.dp(12), 0);
		v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(32)));
		v.setGravity(Gravity.CENTER);
		v.setBackgroundResource(R.drawable.bg_toggle_button);
		v.setText('#'+hashtag);
		return v;
	}

	private void onFilterChanged(int index, boolean checked){
		switch(index){
			case 0 -> showReplies=checked;
			case 1 -> showBoosts=checked;
		}
		GetAccountStatuses.Filter newFilter;
		int newText;
		if(showReplies && showBoosts){
			newFilter=GetAccountStatuses.Filter.INCLUDE_REPLIES;
			newText=R.string.timeline_filter_all_activity;
		}else if(showReplies){
			newFilter=GetAccountStatuses.Filter.OWN_POSTS_AND_REPLIES;
			newText=R.string.timeline_filter_posts_and_replies;
		}else if(showBoosts){
			newFilter=GetAccountStatuses.Filter.DEFAULT;
			newText=R.string.timeline_filter_posts_and_boosts;
		}else{
			newFilter=GetAccountStatuses.Filter.NO_REBLOGS;
			newText=R.string.timeline_filter_posts;
		}
		setFilter(newFilter);
		headerFilterTitle.setText(newText);
	}

	private void setFilter(GetAccountStatuses.Filter newFilter){
		if(newFilter==filter)
			return;
		// TODO maybe cache the filtered timelines that were already loaded?
		filter=newFilter;
		reloadWithNewFilter();
	}

	private void onHashtagClick(View v){
		String tag=(String)v.getTag();
		for(int i=0;i<hashtagsView.getChildCount();i++){
			if(hashtagsView.getChildAt(i) instanceof Checkable checkable){
				checkable.setChecked(false);
			}
		}
		Checkable button=(Checkable) v;
		if(Objects.equals(hashtagFilter, tag)){
			hashtagFilter=null;
		}else{
			hashtagFilter=tag;
			button.setChecked(true);
		}
		reloadWithNewFilter();
	}

	private void reloadWithNewFilter(){
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		data.clear();
		preloadedData.clear();
		int size=displayItems.size();
		displayItems.clear();
		adapter.notifyItemRangeRemoved(0, size);
		loaded=false;
		dataLoading=true;
		doLoadData();
	}
}
