package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetBookmarkedStatuses;
import org.joinmastodon.android.api.requests.statuses.GetFavoritedStatuses;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Status;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class SavedPostsTimelineFragment extends StatusListFragment{
	private Mode mode;
	private String nextPageMaxID;

	@Override
	public void onAttach(Activity activity){
		mode=getArguments().getBoolean("isFavorites") ? Mode.FAVORITES : Mode.BOOKMARKS;
		setTitle(switch(mode){
			case FAVORITES -> R.string.your_favorites;
			case BOOKMARKS -> R.string.bookmarks;
		});
		super.onAttach(activity);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=(switch(mode){
			case FAVORITES -> new GetFavoritedStatuses(offset>0 ? nextPageMaxID : null, count);
			case BOOKMARKS -> new GetBookmarkedStatuses(offset>0 ? nextPageMaxID : null, count);
		}).setCallback(new SimpleCallback<>(this){
			@Override
			public void onSuccess(HeaderPaginationList<Status> result){
				if(getActivity()==null)
					return;
				onDataLoaded(result, result.nextPageUri!=null);
				nextPageMaxID=result.getNextPageMaxID();
			}
		}).exec(accountID);
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
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	private enum Mode{
		FAVORITES,
		BOOKMARKS
	}
}
