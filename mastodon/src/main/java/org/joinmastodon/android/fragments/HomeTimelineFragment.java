package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.fragments.settings.SettingsMainFragment;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineMarkers;
import org.joinmastodon.android.ui.displayitems.GapStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class HomeTimelineFragment extends StatusListFragment{
	private ImageButton fab;
	private ImageView toolbarLogo;
	private Button toolbarShowNewPostsBtn;
	private boolean newPostsBtnShown;
	private AnimatorSet currentNewPostsAnim;

	private String maxID;
	private String lastSavedMarkerID;

	public HomeTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getHomeTimeline(offset>0 ? maxID : null, count, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(CacheablePaginatedResponse<List<Status>> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.items, !result.items.isEmpty());
						maxID=result.maxID;
						if(result.isFromCache())
							loadNewPosts();
					}
				});
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setOnClickListener(this::onFabClick);
		updateToolbarLogo();
		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(newPostsBtnShown && list.getChildAdapterPosition(list.getChildAt(0))<=getMainAdapterOffset()){
					hideNewPostsButton();
				}
			}
		});

		if(GithubSelfUpdater.needSelfUpdating()){
			E.register(this);
			updateUpdateState(GithubSelfUpdater.getInstance().getState());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.home, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), SettingsMainFragment.class, args);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbarLogo();
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad")){
			if(!loaded && !dataLoading){
				loadData();
			}else if(!dataLoading){
				loadNewPosts();
			}
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(!data.isEmpty()){
			String topPostID=displayItems.get(Math.max(0, list.getChildAdapterPosition(list.getChildAt(0))-getMainAdapterOffset())).parentID;
			if(!topPostID.equals(lastSavedMarkerID)){
				lastSavedMarkerID=topPostID;
				new SaveMarkers(topPostID, null)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(TimelineMarkers result){
							}

							@Override
							public void onError(ErrorResponse error){
								lastSavedMarkerID=null;
							}
						})
						.exec(accountID);
			}
		}
	}

	public void onStatusCreated(StatusCreatedEvent ev){
		prependItems(Collections.singletonList(ev.status), true);
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void loadNewPosts(){
		dataLoading=true;
		// The idea here is that we request the timeline such that if there are fewer than `limit` posts,
		// we'll get the currently topmost post as last in the response. This way we know there's no gap
		// between the existing and newly loaded parts of the timeline.
		String sinceID=data.size()>1 ? data.get(1).id : "1";
		currentRequest=new GetHomeTimeline(null, null, 20, sinceID)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Status> result){
						currentRequest=null;
						dataLoading=false;
						if(result.isEmpty() || getActivity()==null)
							return;
						Status last=result.get(result.size()-1);
						List<Status> toAdd;
						if(!data.isEmpty() && last.id.equals(data.get(0).id)){ // This part intersects with the existing one
							toAdd=result.subList(0, result.size()-1); // Remove the already known last post
						}else{
							result.get(result.size()-1).hasGapAfter=true;
							toAdd=result;
						}
						AccountSessionManager.get(accountID).filterStatuses(toAdd, FilterContext.HOME);
						if(!toAdd.isEmpty()){
							prependItems(toAdd, true);
							showNewPostsButton();
							AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(toAdd, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						dataLoading=false;
					}
				})
				.exec(accountID);
	}

	@Override
	public void onGapClick(GapStatusDisplayItem.Holder item){
		if(dataLoading)
			return;
		item.getItem().loading=true;
		V.setVisibilityAnimated(item.progress, View.VISIBLE);
		V.setVisibilityAnimated(item.text, View.GONE);
		GapStatusDisplayItem gap=item.getItem();
		dataLoading=true;
		currentRequest=new GetHomeTimeline(item.getItemID(), null, 20, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Status> result){
						currentRequest=null;
						dataLoading=false;
						if(getActivity()==null)
							return;
						int gapPos=displayItems.indexOf(gap);
						if(gapPos==-1)
							return;
						if(result.isEmpty()){
							displayItems.remove(gapPos);
							adapter.notifyItemRemoved(getMainAdapterOffset()+gapPos);
							Status gapStatus=getStatusByID(gap.parentID);
							if(gapStatus!=null){
								gapStatus.hasGapAfter=false;
								AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(Collections.singletonList(gapStatus), false);
							}
						}else{
							Set<String> idsBelowGap=new HashSet<>();
							boolean belowGap=false;
							int gapPostIndex=0;
							for(Status s:data){
								if(belowGap){
									idsBelowGap.add(s.id);
								}else if(s.id.equals(gap.parentID)){
									belowGap=true;
									s.hasGapAfter=false;
									AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(Collections.singletonList(s), false);
								}else{
									gapPostIndex++;
								}
							}
							int endIndex=0;
							for(Status s:result){
								endIndex++;
								if(idsBelowGap.contains(s.id))
									break;
							}
							if(endIndex==result.size()){
								result.get(result.size()-1).hasGapAfter=true;
							}else{
								result=result.subList(0, endIndex);
							}
							List<StatusDisplayItem> targetList=displayItems.subList(gapPos, gapPos+1);
							targetList.clear();
							List<Status> insertedPosts=data.subList(gapPostIndex+1, gapPostIndex+1);
							for(Status s:result){
								if(idsBelowGap.contains(s.id))
									break;
								targetList.addAll(buildDisplayItems(s));
								insertedPosts.add(s);
							}
							AccountSessionManager.get(accountID).filterStatuses(insertedPosts, FilterContext.HOME);
							if(targetList.isEmpty()){
								// oops. We didn't add new posts, but at least we know there are none.
								adapter.notifyItemRemoved(getMainAdapterOffset()+gapPos);
							}else{
								adapter.notifyItemChanged(getMainAdapterOffset()+gapPos);
								adapter.notifyItemRangeInserted(getMainAdapterOffset()+gapPos+1, targetList.size()-1);
							}
							AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(insertedPosts, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						dataLoading=false;
						gap.loading=false;
						Activity a=getActivity();
						if(a!=null){
							error.showToast(a);
							int gapPos=displayItems.indexOf(gap);
							if(gapPos>=0)
								adapter.notifyItemChanged(gapPos);
						}
					}
				})
				.exec(accountID);

	}

	@Override
	public void onRefresh(){
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
			dataLoading=false;
		}
		super.onRefresh();
	}

	private void updateToolbarLogo(){
		toolbarLogo=new ImageView(getActivity());
		toolbarLogo.setScaleType(ImageView.ScaleType.CENTER);
		toolbarLogo.setImageResource(R.drawable.logo);
		toolbarLogo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)));

		toolbarShowNewPostsBtn=new Button(getActivity());
		toolbarShowNewPostsBtn.setTextAppearance(R.style.m3_title_medium);
		toolbarShowNewPostsBtn.setTextColor(0xffffffff);
		toolbarShowNewPostsBtn.setStateListAnimator(null);
		toolbarShowNewPostsBtn.setBackgroundResource(R.drawable.bg_button_new_posts);
		toolbarShowNewPostsBtn.setText(R.string.see_new_posts);
		toolbarShowNewPostsBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_fluent_arrow_up_16_filled, 0, 0, 0);
		toolbarShowNewPostsBtn.setCompoundDrawableTintList(toolbarShowNewPostsBtn.getTextColors());
		toolbarShowNewPostsBtn.setCompoundDrawablePadding(V.dp(8));
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N)
			UiUtils.fixCompoundDrawableTintOnAndroid6(toolbarShowNewPostsBtn);
		toolbarShowNewPostsBtn.setOnClickListener(this::onNewPostsBtnClick);

		if(newPostsBtnShown){
			toolbarShowNewPostsBtn.setVisibility(View.VISIBLE);
			toolbarLogo.setVisibility(View.INVISIBLE);
			toolbarLogo.setAlpha(0f);
		}else{
			toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
			toolbarShowNewPostsBtn.setAlpha(0f);
			toolbarShowNewPostsBtn.setScaleX(.8f);
			toolbarShowNewPostsBtn.setScaleY(.8f);
			toolbarLogo.setVisibility(View.VISIBLE);
		}

		FrameLayout logoWrap=new FrameLayout(getActivity());
		logoWrap.addView(toolbarLogo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		logoWrap.addView(toolbarShowNewPostsBtn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(32), Gravity.CENTER));

		Toolbar toolbar=getToolbar();
		toolbar.addView(logoWrap, new Toolbar.LayoutParams(Gravity.CENTER));
	}

	private void showNewPostsButton(){
		if(newPostsBtnShown)
			return;
		newPostsBtnShown=true;
		if(currentNewPostsAnim!=null){
			currentNewPostsAnim.cancel();
		}
		toolbarShowNewPostsBtn.setVisibility(View.VISIBLE);
		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(toolbarLogo, View.ALPHA, 0f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, 1f)
		);
		set.setDuration(300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				toolbarLogo.setVisibility(View.INVISIBLE);
				currentNewPostsAnim=null;
			}
		});
		currentNewPostsAnim=set;
		set.start();
	}

	private void hideNewPostsButton(){
		if(!newPostsBtnShown)
			return;
		newPostsBtnShown=false;
		if(currentNewPostsAnim!=null){
			currentNewPostsAnim.cancel();
		}
		toolbarLogo.setVisibility(View.VISIBLE);
		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(toolbarLogo, View.ALPHA, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 0f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, .8f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, .8f)
		);
		set.setDuration(300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
				currentNewPostsAnim=null;
			}
		});
		currentNewPostsAnim=set;
		set.start();
	}

	private void onNewPostsBtnClick(View v){
		if(newPostsBtnShown){
			hideNewPostsButton();
			scrollToTop();
		}
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		if(GithubSelfUpdater.needSelfUpdating()){
			E.unregister(this);
		}
	}

	private void updateUpdateState(GithubSelfUpdater.UpdateState state){
		if(state!=GithubSelfUpdater.UpdateState.NO_UPDATE && state!=GithubSelfUpdater.UpdateState.CHECKING)
			getToolbar().getMenu().findItem(R.id.settings).setIcon(R.drawable.ic_settings_updateready_24px);
	}

	@Subscribe
	public void onSelfUpdateStateChanged(SelfUpdateStateChangedEvent ev){
		updateUpdateState(ev.state);
	}

	@Override
	protected boolean shouldRemoveAccountPostsWhenUnfollowing(){
		return true;
	}
}
