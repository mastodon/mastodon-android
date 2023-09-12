package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.tags.GetTag;
import org.joinmastodon.android.api.requests.tags.SetTagFollowed;
import org.joinmastodon.android.api.requests.timelines.GetHashtagTimeline;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.parceler.Parcels;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class HashtagTimelineFragment extends StatusListFragment{
	private Hashtag hashtag;
	private String hashtagName;
	private ImageButton fab;
	private TextView headerTitle, headerSubtitle;
	private ProgressBarButton followButton;
	private ProgressBar followProgress;
	private MenuItem followMenuItem;
	private boolean followRequestRunning;
	private boolean toolbarContentVisible;

	public HashtagTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		if(getArguments().containsKey("hashtag")){
			hashtag=Parcels.unwrap(getArguments().getParcelable("hashtag"));
			hashtagName=hashtag.name;
		}else{
			hashtagName=getArguments().getString("hashtagName");
		}
		setTitle('#'+hashtagName);
		setHasOptionsMenu(true);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetHashtagTimeline(hashtagName, offset==0 ? null : getMaxID(), null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
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

	@Override
	public void loadData(){
		reloadTag();
		super.loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setOnClickListener(this::onFabClick);

		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				View topChild=recyclerView.getChildAt(0);
				int firstChildPos=recyclerView.getChildAdapterPosition(topChild);
				float newAlpha=firstChildPos>0 ? 1f : Math.min(1f, -topChild.getTop()/(float)headerTitle.getHeight());
				toolbarTitleView.setAlpha(newAlpha);
				boolean newToolbarVisibility=newAlpha>0.5f;
				if(newToolbarVisibility!=toolbarContentVisible){
					toolbarContentVisible=newToolbarVisibility;
					if(followMenuItem!=null)
						followMenuItem.setVisible(toolbarContentVisible);
				}
			}
		});
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("prefilledText", '#'+hashtagName+' ');
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	@Override
	protected void onSetFabBottomInset(int inset){
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+inset;
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View header=getActivity().getLayoutInflater().inflate(R.layout.header_hashtag_timeline, list, false);
		headerTitle=header.findViewById(R.id.title);
		headerSubtitle=header.findViewById(R.id.subtitle);
		followButton=header.findViewById(R.id.profile_action_btn);
		followProgress=header.findViewById(R.id.action_progress);

		headerTitle.setText("#"+hashtagName);
		followButton.setVisibility(View.GONE);
		followButton.setOnClickListener(v->{
			if(hashtag==null)
				return;
			setFollowed(!hashtag.following);
		});
		updateHeader();

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(header));
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@Override
	protected int getMainAdapterOffset(){
		return 1;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		followMenuItem=menu.add(getString(hashtag!=null && hashtag.following ? R.string.unfollow_user : R.string.follow_user, "#"+hashtagName));
		followMenuItem.setVisible(toolbarContentVisible);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(hashtag!=null){
			setFollowed(!hashtag.following);
		}
		return true;
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		toolbarTitleView.setAlpha(toolbarContentVisible ? 1f : 0f);
		if(followMenuItem!=null)
			followMenuItem.setVisible(toolbarContentVisible);
	}

	private void updateHeader(){
		if(hashtag==null)
			return;

		if(hashtag.history!=null && !hashtag.history.isEmpty()){
			int weekPosts=hashtag.history.stream().mapToInt(h->h.uses).sum();
			int todayPosts=hashtag.history.get(0).uses;
			int numAccounts=hashtag.history.stream().mapToInt(h->h.accounts).sum();
			int hSpace=V.dp(8);
			SpannableStringBuilder ssb=new SpannableStringBuilder();
			ssb.append(getResources().getQuantityString(R.plurals.x_posts, weekPosts, weekPosts));
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append('·');
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append(getResources().getQuantityString(R.plurals.x_participants, numAccounts, numAccounts));
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append('·');
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append(getResources().getQuantityString(R.plurals.x_posts_today, todayPosts, todayPosts));
			headerSubtitle.setText(ssb);
		}

		int styleRes;
		followButton.setVisibility(View.VISIBLE);
		if(hashtag.following){
			followButton.setText(R.string.button_following);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal;
		}else{
			followButton.setText(R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}
		TypedArray ta=followButton.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		followButton.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=followButton.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		followButton.setTextColor(ta.getColorStateList(0));
		followProgress.setIndeterminateTintList(ta.getColorStateList(0));
		ta.recycle();

		followButton.setTextVisible(true);
		followProgress.setVisibility(View.GONE);
		if(followMenuItem!=null){
			followMenuItem.setTitle(getString(hashtag.following ? R.string.unfollow_user : R.string.follow_user, "#"+hashtagName));
		}
	}

	private void reloadTag(){
		new GetTag(hashtagName)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag result){
						hashtag=result;
						updateHeader();
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	private void setFollowed(boolean followed){
		if(followRequestRunning)
			return;
		followButton.setTextVisible(false);
		followProgress.setVisibility(View.VISIBLE);
		followRequestRunning=true;
		new SetTagFollowed(hashtagName, followed)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag result){
						if(getActivity()==null)
							return;
						hashtag=result;
						updateHeader();
						followRequestRunning=false;
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						if(error instanceof MastodonErrorResponse er && "Duplicate record".equals(er.error)){
							hashtag.following=true;
						}else{
							error.showToast(getActivity());
						}
						updateHeader();
						followRequestRunning=false;
					}
				})
				.exec(accountID);
	}
}
