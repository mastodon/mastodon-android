package org.joinmastodon.android.fragments.discover;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.SearchResultViewModel;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.SearchViewHelper;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.CustomTransitionsFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class SearchQueryFragment extends MastodonRecyclerFragment<SearchResultViewModel> implements CustomTransitionsFragment, OnBackPressedListener{
	private static final Pattern HASHTAG_REGEX=Pattern.compile("^(\\w*[a-zA-Z·]\\w*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern USERNAME_REGEX=Pattern.compile("^@?([a-z0-9_-]+)(@[^\\s]+)?$", Pattern.CASE_INSENSITIVE);

	private MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
	private HideableSingleViewRecyclerAdapter recentsHeader;
	private ListItem<Void> openUrlItem, goToHashtagItem, goToAccountItem, goToStatusSearchItem, goToAccountSearchItem;
	private ArrayList<ListItem<Void>> topOptions=new ArrayList<>();
	private GenericListItemsAdapter<Void> topOptionsAdapter;

	private String accountID;
	private SearchViewHelper searchViewHelper;
	private String currentQuery;
	private LayerDrawable navigationIcon;
	private Drawable searchIcon, backIcon;

	public SearchQueryFragment(){
		super(20);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setRefreshEnabled(false);
		setEmptyText("");

		openUrlItem=new ListItem<>(R.string.search_open_url, 0, R.drawable.ic_link_24px, this::onOpenURLClick);
		goToHashtagItem=new ListItem<>("", null, R.drawable.ic_tag_24px, this::onGoToHashtagClick);
		goToAccountItem=new ListItem<>("", null, R.drawable.ic_person_24px, this::onGoToAccountClick);
		goToStatusSearchItem=new ListItem<>("", null, R.drawable.ic_search_24px, this::onGoToStatusSearchClick);
		goToAccountSearchItem=new ListItem<>("", null, R.drawable.ic_group_24px, this::onGoToAccountSearchClick);
		currentQuery=getArguments().getString("query");

		dataLoaded();
		doLoadData(0, 0);
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(isInRecentMode()){
			AccountSessionManager.getInstance().getAccount(accountID).getCacheController().getRecentSearches(results->{
				if(getActivity()==null)
					return;

				onDataLoaded(results.stream().map(sr->{
					SearchResultViewModel vm=new SearchResultViewModel(sr, accountID, true);
					if(sr.type==SearchResult.Type.HASHTAG){
						vm.hashtagItem.onClick=()->openHashtag(sr);
					}
					return vm;
				}).collect(Collectors.toList()), false);
				recentsHeader.setVisible(!data.isEmpty());
			});
		}else{
			currentRequest=new GetSearchResults(currentQuery, null, false)
					.limit(2)
					.setCallback(new SimpleCallback<>(this){
						@Override
						public void onSuccess(SearchResults result){
							onDataLoaded(Stream.of(result.hashtags.stream().map(SearchResult::new), result.accounts.stream().map(SearchResult::new))
									.flatMap(Function.identity())
									.map(sr->{
										SearchResultViewModel vm=new SearchResultViewModel(sr, accountID, false);
										if(sr.type==SearchResult.Type.HASHTAG){
											vm.hashtagItem.onClick=()->openHashtag(sr);
										}
										return vm;
									})
									.collect(Collectors.toList()), false);
							recentsHeader.setVisible(false);
						}
					})
					.exec(accountID);
		}
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		View header=getActivity().getLayoutInflater().inflate(R.layout.display_item_section_header, list, false);
		TextView title=header.findViewById(R.id.title);
		Button action=header.findViewById(R.id.action_btn);
		title.setText(R.string.recent_searches);
		action.setText(R.string.clear_all);
		action.setOnClickListener(v->onClearRecentClick());
		recentsHeader=new HideableSingleViewRecyclerAdapter(header);

		mergeAdapter.addAdapter(recentsHeader);
		mergeAdapter.addAdapter(topOptionsAdapter=new GenericListItemsAdapter<>(topOptions));
		mergeAdapter.addAdapter(new SearchResultsAdapter());
		return mergeAdapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		searchViewHelper=new SearchViewHelper(getActivity(), getToolbarContext(), getString(R.string.search_mastodon));
		searchViewHelper.setListeners(this::onQueryChanged, this::onQueryChangedNoDebounce);
		searchViewHelper.addDivider(contentView);
		searchViewHelper.setEnterCallback(this::onSearchViewEnter);

		navigationIcon=new LayerDrawable(new Drawable[]{
				searchIcon=getToolbarContext().getResources().getDrawable(R.drawable.ic_search_24px, getToolbarContext().getTheme()).mutate(),
				backIcon=getToolbarContext().getResources().getDrawable(R.drawable.ic_arrow_back, getToolbarContext().getTheme()).mutate()
		}){
			@Override
			public Drawable mutate(){
				return this;
			}
		};

		super.onViewCreated(view, savedInstanceState);

		view.setBackgroundResource(R.drawable.bg_m3_surface3);
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Surface, R.attr.colorM3Primary, 0.11f);
		setStatusBarColor(color);
		setNavigationBarColor(color);
		if(currentQuery!=null){
			searchViewHelper.setQuery(currentQuery);
			searchIcon.setAlpha(0);
		}
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3OutlineVariant, 1, 0, 0, vh->!isInRecentMode() && vh.getAbsoluteAdapterPosition()==topOptions.size()-1));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		((ViewGroup.MarginLayoutParams)getToolbar().getLayoutParams()).topMargin=V.dp(8);
		searchViewHelper.install(getToolbar());
	}

	@Override
	protected boolean wantsElevationOnScrollEffect(){
		return false;
	}

	private void onQueryChanged(String q){
		currentQuery=q;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		refreshing=true;
		doLoadData(0, 0);
	}

	private void onQueryChangedNoDebounce(String q){
		updateTopOptions(q);
		if(!TextUtils.isEmpty(q)){
			recentsHeader.setVisible(false);
		}
		data.clear();
		mergeAdapter.notifyDataSetChanged();
	}

	private void updateTopOptions(String q){
		topOptions.clear();
		// https://github.com/mastodon/mastodon/blob/a985d587e13494b78ef2879e4d97f78a2df693db/app/javascript/mastodon/features/compose/components/search.jsx#L233
		String trimmedValue=q.trim();
		if(trimmedValue.length()>0){
			boolean couldBeURL=trimmedValue.startsWith("https://") && !trimmedValue.contains(" ");
			if(couldBeURL){
				topOptions.add(openUrlItem);
			}

			boolean couldBeHashtag=(trimmedValue.startsWith("#") && trimmedValue.length()>1 && !trimmedValue.contains(" ")) || HASHTAG_REGEX.matcher(trimmedValue).find();
			if(couldBeHashtag){
				String tag=trimmedValue.startsWith("#") ? trimmedValue.substring(1) : trimmedValue;
				goToHashtagItem.title=getString(R.string.posts_matching_hashtag, "#"+tag);
				topOptions.add(goToHashtagItem);
			}

			Matcher usernameMatcher=USERNAME_REGEX.matcher(trimmedValue);
			if(usernameMatcher.find()){
				String username="@"+usernameMatcher.group(1);
				String atDomain=usernameMatcher.group(2);
				if(atDomain==null){
					username+="@"+AccountSessionManager.get(accountID).domain;
				}
				goToAccountItem.title=getString(R.string.search_go_to_account, username);
				topOptions.add(goToAccountItem);
			}

			goToStatusSearchItem.title=getString(R.string.posts_matching_string, trimmedValue);
			topOptions.add(goToStatusSearchItem);
			goToAccountSearchItem.title=getString(R.string.accounts_matching_string, trimmedValue);
			topOptions.add(goToAccountSearchItem);
		}
		topOptionsAdapter.notifyDataSetChanged();
	}

	@Override
	public Animator onCreateEnterTransition(View prev, View container){
		return createTransition(prev, container, true);
	}

	@Override
	public Animator onCreateExitTransition(View prev, View container){
		return createTransition(prev, container, false);
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
	}

	@Override
	protected Drawable getNavigationIconDrawable(){
		return navigationIcon;
	}

	@Override
	protected void onShown(){
		super.onShown();
		getActivity().getSystemService(InputMethodManager.class).showSoftInput(getActivity().getCurrentFocus(), 0);
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
	}

	private Animator createTransition(View prev, View container, boolean enter){
		int[] loc={0, 0};
		View searchBtn=prev.findViewById(R.id.search_wrap);
		searchBtn.getLocationInWindow(loc);
		int btnLeft=loc[0], btnTop=loc[1];
		container.getLocationInWindow(loc);
		int offX=btnLeft-loc[0], offY=btnTop-loc[1];

		float screenRadius;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
			WindowInsets insets=container.getRootWindowInsets();
			screenRadius=Math.min(
					Math.min(insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT).getRadius(), insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT).getRadius()),
					Math.min(insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT).getRadius(), insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT).getRadius())
			);
		}else{
			screenRadius=0;
		}
		float buttonRadius=V.dp(26);

		Rect buttonBounds=new Rect(offX, offY, offX+searchBtn.getWidth(), offY+searchBtn.getHeight());
		Rect containerBounds=new Rect(0, 0, container.getWidth(), container.getHeight());
		AnimatableOutlineProvider outlineProvider=new AnimatableOutlineProvider(enter ? buttonBounds : containerBounds, enter ? containerBounds : buttonBounds, enter ? buttonRadius : screenRadius);
		container.setOutlineProvider(outlineProvider);
		container.setClipToOutline(true);

		AnimatorSet set=new AnimatorSet();
		ObjectAnimator boundsAnim;

		Toolbar toolbar=getToolbar();
		float toolbarTX=offX-toolbar.getX();
		float toolbarTY=offY-toolbar.getY()+(searchBtn.getHeight()-toolbar.getHeight())/2f;
		ArrayList<Animator> anims=new ArrayList<>();
		anims.add(boundsAnim=ObjectAnimator.ofFloat(outlineProvider, "boundsFraction", 0f, 1f));
		anims.add(ObjectAnimator.ofFloat(outlineProvider, "radius", enter ? buttonRadius : screenRadius, enter ? screenRadius : buttonRadius));
		anims.add(ObjectAnimator.ofFloat(toolbar, View.TRANSLATION_X, enter ? toolbarTX : 0, enter ? 0 : toolbarTX));
		anims.add(ObjectAnimator.ofFloat(toolbar, View.TRANSLATION_Y, enter ? toolbarTY : 0, enter ? 0 : toolbarTY));
		anims.add(ObjectAnimator.ofFloat(searchViewHelper.getSearchLayout(), View.TRANSLATION_X, enter ? V.dp(-4) : 0, enter ? 0 : V.dp(-4)));
		anims.add(ObjectAnimator.ofFloat(searchViewHelper.getDivider(), View.ALPHA, enter ? 0 : 1, enter ? 1 : 0));
		View parentContent=prev.findViewById(R.id.discover_content);
		View parentContentParent=(View) parentContent.getParent();
		parentContentParent.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		if(enter){
			anims.add(ObjectAnimator.ofFloat(contentWrap, View.TRANSLATION_Y, V.dp(-16), 0));
		}else{
		}
		anims.add(ObjectAnimator.ofFloat(contentWrap, View.ALPHA, enter ? 0 : 1, enter ? 1 : 0));
		for(Animator anim:anims){
			anim.setDuration(enter ? 700 : 300);
		}
		if(TextUtils.isEmpty(currentQuery)){
			anims.add(ObjectAnimator.ofInt(searchIcon, "alpha", enter ? 255 : 0, enter ? 0 : 255).setDuration(200));
			anims.add(ObjectAnimator.ofInt(backIcon, "alpha", enter ? 0 : 255, enter ? 255 : 0).setDuration(200));
		}
		ObjectAnimator parentContentFade;
		anims.add(parentContentFade=ObjectAnimator.ofFloat(parentContent, View.ALPHA, enter ? 1 : 0, enter ? 0 : 1).setDuration(enter ? 350 : 250));
		if(!enter){
			parentContentFade.setStartDelay(50);
			ObjectAnimator parentContentTY;
			anims.add(parentContentTY=ObjectAnimator.ofFloat(parentContent, View.TRANSLATION_Y, V.dp(16), 0).setDuration(250));
			parentContentTY.setStartDelay(50);
		}

		set.playTogether(anims);
		set.setInterpolator(AnimationUtils.loadInterpolator(getActivity(), R.interpolator.m3_sys_motion_easing_emphasized_decelerate));
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				container.setOutlineProvider(null);
				container.setClipToOutline(false);
				parentContentParent.setBackground(null);
			}
		});
		boundsAnim.addUpdateListener(animation->{
			container.invalidateOutline();
			navigationIcon.invalidateSelf();
		});
		return set;
	}

	private void openHashtag(SearchResult res){
		UiUtils.openHashtagTimeline(getActivity(), accountID, res.hashtag.name);
		AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putRecentSearch(res);
	}

	private boolean isInRecentMode(){
		return TextUtils.isEmpty(currentQuery);
	}

	private void onSearchViewEnter(){
		deliverResult(currentQuery, null);
	}

	private void onOpenURLClick(){
		((MainActivity)getActivity()).handleURL(Uri.parse(searchViewHelper.getQuery()), accountID);
	}

	private void onGoToHashtagClick(){
		String q=searchViewHelper.getQuery();
		if(q.startsWith("#"))
			q=q.substring(1);
		UiUtils.openHashtagTimeline(getActivity(), accountID, q);
	}

	private void onGoToAccountClick(){
		String q=searchViewHelper.getQuery();
		if(!q.startsWith("@")){
			q="@"+q;
		}
		if(q.lastIndexOf('@')==0){
			q+="@"+AccountSessionManager.get(accountID).domain;
		}
		((MainActivity)getActivity()).openSearchQuery(q, accountID, R.string.loading, true);
	}

	private void onGoToStatusSearchClick(){
		deliverResult(searchViewHelper.getQuery(), SearchResult.Type.STATUS);
	}

	private void onGoToAccountSearchClick(){
		deliverResult(searchViewHelper.getQuery(), SearchResult.Type.ACCOUNT);
	}

	private void onClearRecentClick(){
		AccountSessionManager.getInstance().getAccount(accountID).getCacheController().clearRecentSearches();
		if(isInRecentMode()){
			data.clear();
			recentsHeader.setVisible(false);
			mergeAdapter.notifyDataSetChanged();
		}
	}

	private void deliverResult(String query, SearchResult.Type typeFilter){
		Bundle res=new Bundle();
		res.putString("query", query);
		if(typeFilter!=null)
			res.putInt("filter", typeFilter.ordinal());
		setResult(true, res);
		Nav.finish(this);
	}

	@Override
	public boolean onBackPressed(){
		String initialQuery=getArguments().getString("query");
		searchViewHelper.setQuery(TextUtils.isEmpty(initialQuery) ? "" : initialQuery);
		currentQuery=initialQuery;
		return false;
	}

	private static class AnimatableOutlineProvider extends ViewOutlineProvider{
		private float boundsFraction, radius;
		private final Rect boundsFrom, boundsTo;

		private AnimatableOutlineProvider(Rect boundsFrom, Rect boundsTo, float radius){
			this.boundsFrom=boundsFrom;
			this.boundsTo=boundsTo;
			this.radius=radius;
		}

		@Override
		public void getOutline(View view, Outline outline){
			outline.setRoundRect(
					UiUtils.lerp(boundsFrom.left, boundsTo.left, boundsFraction),
					UiUtils.lerp(boundsFrom.top, boundsTo.top, boundsFraction),
					UiUtils.lerp(boundsFrom.right, boundsTo.right, boundsFraction),
					UiUtils.lerp(boundsFrom.bottom, boundsTo.bottom, boundsFraction),
					radius
			);
		}

		@Keep
		public float getBoundsFraction(){
			return boundsFraction;
		}

		@Keep
		public void setBoundsFraction(float boundsFraction){
			this.boundsFraction=boundsFraction;
		}

		@Keep
		public float getRadius(){
			return radius;
		}

		@Keep
		public void setRadius(float radius){
			this.radius=radius;
		}
	}

	private class SearchResultsAdapter extends UsableRecyclerView.Adapter<RecyclerView.ViewHolder> implements ImageLoaderRecyclerAdapter{
		public SearchResultsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			if(viewType==R.id.list_item_account){
				return new CustomAccountViewHolder(SearchQueryFragment.this, parent, null);
			}else if(viewType==R.id.list_item_simple){
				return new SimpleListItemViewHolder(parent.getContext(), parent);
			}
			throw new IllegalArgumentException();
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
			if(holder instanceof CustomAccountViewHolder avh){
				avh.bind(data.get(position).account);
				avh.searchResult=data.get(position).result;
			}else if(holder instanceof SimpleListItemViewHolder ivh){
				ivh.bind(data.get(position).hashtagItem);
			}
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public int getItemViewType(int position){
			return switch(data.get(position).result.type){
				case ACCOUNT -> R.id.list_item_account;
				case HASHTAG -> R.id.list_item_simple;
				default -> throw new IllegalStateException("Unexpected value: "+data.get(position).result.type);
			};
		}

		@Override
		public int getImageCountForItem(int position){
			SearchResultViewModel vm=data.get(position);
			if(vm.account!=null)
				return vm.account.emojiHelper.getImageCount()+1;
			return 0;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			SearchResultViewModel vm=data.get(position);
			if(vm.account!=null){
				if(image==0)
					return vm.account.avaRequest;
				return vm.account.emojiHelper.getImageRequest(image-1);
			}
			return null;
		}
	}

	private class CustomAccountViewHolder extends AccountViewHolder{
		public SearchResult searchResult;

		public CustomAccountViewHolder(Fragment fragment, ViewGroup list, HashMap<String, Relationship> relationships){
			super(fragment, list, relationships);
			setStyle(AccessoryType.NONE, false);
		}

		@Override
		public void onClick(){
			super.onClick();
			AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putRecentSearch(searchResult);
		}
	}
}
