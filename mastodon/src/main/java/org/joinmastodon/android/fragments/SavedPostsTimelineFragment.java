package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetBookmarkedStatuses;
import org.joinmastodon.android.api.requests.statuses.GetFavoritedStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.views.FilterChipView;
import org.parceler.Parcels;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SavedPostsTimelineFragment extends StatusListFragment{
	private Account user;
	private Mode mode;
	private HorizontalScrollView filtersBar;
	private FilterChipView favoritesChip, bookmarksChip;

	public SavedPostsTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	public static SavedPostsTimelineFragment newInstance(String accountID, Account profileAccount, boolean load){
		SavedPostsTimelineFragment f=new SavedPostsTimelineFragment();
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
		mode=Mode.FAVORITES;
		super.onAttach(activity);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=(switch(mode){
			case FAVORITES -> new GetFavoritedStatuses(offset>0 ? getMaxID() : null, count);
			case BOOKMARKS -> new GetBookmarkedStatuses(offset>0 ? getMaxID() : null, count);
		}).setCallback(new SimpleCallback<>(this){
			@Override
			public void onSuccess(HeaderPaginationList<Status> result){
				if(getActivity()==null)
					return;
				onDataLoaded(result, result.nextPageUri!=null);
			}
		}).exec(accountID);
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

	protected void onStatusCreated(Status status){
		// no-op
	}

	@Override
	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		// no-op
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		filtersBar=new HorizontalScrollView(getActivity());
		LinearLayout filtersLayout=new LinearLayout(getActivity());
		filtersBar.addView(filtersLayout);
		filtersLayout.setOrientation(LinearLayout.HORIZONTAL);
		filtersLayout.setPadding(V.dp(16), 0, V.dp(16), V.dp(8));
		filtersLayout.setDividerDrawable(new EmptyDrawable(V.dp(8), 1));
		filtersLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

		favoritesChip=new FilterChipView(getActivity());
		favoritesChip.setText(R.string.your_favorites);
		favoritesChip.setTag(Mode.FAVORITES);
		favoritesChip.setSelected(mode==Mode.FAVORITES);
		favoritesChip.setOnClickListener(this::onFilterClick);
		filtersLayout.addView(favoritesChip);

		bookmarksChip=new FilterChipView(getActivity());
		bookmarksChip.setText(R.string.bookmarks);
		bookmarksChip.setTag(Mode.BOOKMARKS);
		bookmarksChip.setSelected(mode==Mode.BOOKMARKS);
		bookmarksChip.setOnClickListener(this::onFilterClick);
		filtersLayout.addView(bookmarksChip);

		View banner=getActivity().getLayoutInflater().inflate(R.layout.discover_info_banner, list, false);
		TextView text=banner.findViewById(R.id.banner_text);
		text.setText(R.string.profile_saved_posts_explanation);
		ImageView icon=banner.findViewById(R.id.icon);
		icon.setImageResource(R.drawable.ic_lock_24px);

		// Prevents margins messing up things
		FrameLayout bannerW=new FrameLayout(getActivity());
		bannerW.addView(banner);

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(bannerW));
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(filtersBar));
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	private FilterChipView getViewForMode(Mode mode){
		return switch(mode){
			case FAVORITES -> favoritesChip;
			case BOOKMARKS -> bookmarksChip;
		};
	}

	private void onFilterClick(View v){
		Mode newMode=(Mode) v.getTag();
		if(newMode==mode)
			return;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		getViewForMode(mode).setSelected(false);
		mode=newMode;
		v.setSelected(true);
		data.clear();
		preloadedData.clear();
		int size=displayItems.size();
		displayItems.clear();
		adapter.notifyItemRangeRemoved(0, size);
		loaded=false;
		dataLoading=true;
		doLoadData();
	}

	private enum Mode{
		FAVORITES,
		BOOKMARKS
	}
}
