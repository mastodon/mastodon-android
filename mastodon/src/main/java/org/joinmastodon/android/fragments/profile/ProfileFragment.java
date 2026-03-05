package org.joinmastodon.android.fragments.profile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountFamiliarFollowers;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.requests.accounts.RemoveAccountFromFollowers;
import org.joinmastodon.android.api.requests.accounts.SetAccountEndorsed;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetAccountPersonalNote;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfAccountUpdatedEvent;
import org.joinmastodon.android.fragments.AccountSimpleTimelineFragment;
import org.joinmastodon.android.fragments.AddAccountToListsFragment;
import org.joinmastodon.android.fragments.AssistContentProviderFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ManageFollowedHashtagsFragment;
import org.joinmastodon.android.fragments.SavedPostsTimelineFragment;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.fragments.account_list.FamiliarFollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.fragments.settings.SettingsAccountFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.FamiliarFollowers;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.sheets.DecentralizationExplainerSheet;
import org.joinmastodon.android.ui.sheets.LongProfileFieldSheet;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.ImageSpanThatDoesNotBreakShitForNoGoodReason;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.CustomDrawingOrderLinearLayout;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProfileFieldsGridLayout;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.ui.views.WrappingLinearLayout;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.WindowInsetsAwareFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class ProfileFragment extends LoaderFragment implements ScrollableToTop, AssistContentProviderFragment{

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder;
	private TextView name, username, bio, followersCount, followingCount, postsCount;
	private TextView joinDate;
	private ImageButton usernameHelp;
	private ProgressBarButton actionButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private ProfileFeaturedFragment featuredFragment;
	private AccountTimelineFragment timelineFragment;
	private AccountSimpleTimelineFragment mediaTimelineFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private View followersBtn, followingBtn;
	private ProgressBar actionProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private WrappingLinearLayout countersLayout;
	private View tabsDivider;
	private View actionButtonWrap;
	private CustomDrawingOrderLinearLayout scrollableContent;
	private ImageButton menuButton, notificationsButton;
	private ProgressBar innerProgress;
	private View actions;
	private View familiarFollowersRow;
	private ImageView[] familiarFollowersAvatars;
	private TextView familiarFollowersLabel;
	private WrappingLinearLayout badgesLayout;
	private ProfileFieldsGridLayout fieldsLayout;
	private Button expandFieldsButton;

	private Account account;
	private String accountID;
	private Relationship relationship;
	private boolean isOwnProfile;
	private ArrayList<AccountField> fields=new ArrayList<>();
	private List<Account> familiarFollowers=List.of();

	private String profileAccountID;
	private boolean refreshing;
	private View fab;
	private WindowInsets childInsets;
	private PhotoViewer currentPhotoViewer;
	private ElevationOnScrollListener onScrollListener;
	private Drawable tabsColorBackground;
	private boolean tabBarIsAtTop;
	private Animator tabBarColorAnim;
	private HashSet<APIRequest<?>> relationshipRequests=new HashSet<>();
	private PopupMenu buttonMenu;
	private ArrayList<Tab> tabs=new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		if(getArguments().containsKey("profileAccount")){
			account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
			profileAccountID=account.id;
			isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
			loaded=true;
			if(!isOwnProfile)
				loadRelationship();
		}else{
			profileAccountID=getArguments().getString("profileAccountID");
			if(!getArguments().getBoolean("noAutoLoad", false))
				loadData();
		}
		E.register(this);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(APIRequest<?> req:relationshipRequests)
			req.cancel();
		relationshipRequests.clear();
		E.unregister(this);
	}

	@Override
	protected void onShown(){
		super.onShown();
		syncScrollState();
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile, container, false);

		avatar=content.findViewById(R.id.avatar);
		cover=content.findViewById(R.id.cover);
		avatarBorder=content.findViewById(R.id.avatar_border);
		name=content.findViewById(R.id.name);
		username=content.findViewById(R.id.username);
		usernameHelp=content.findViewById(R.id.username_help);
		bio=content.findViewById(R.id.bio);
		followersCount=content.findViewById(R.id.followers_count);
		followersBtn=content.findViewById(R.id.followers_btn);
		followingCount=content.findViewById(R.id.following_count);
		followingBtn=content.findViewById(R.id.following_btn);
		postsCount=content.findViewById(R.id.posts_count);
		actionButton=content.findViewById(R.id.profile_action_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		actionProgress=content.findViewById(R.id.action_progress);
		fab=content.findViewById(R.id.fab);
		countersLayout=content.findViewById(R.id.profile_counters);
		tabsDivider=content.findViewById(R.id.tabs_divider);
		actionButtonWrap=content.findViewById(R.id.profile_action_btn_wrap);
		scrollableContent=content.findViewById(R.id.scrollable_content);
		menuButton=content.findViewById(R.id.options_btn);
		notificationsButton=content.findViewById(R.id.notifications_btn);
		innerProgress=content.findViewById(R.id.profile_progress);
		actions=content.findViewById(R.id.profile_actions);
		familiarFollowersRow=content.findViewById(R.id.familiar_followers);
		familiarFollowersAvatars=new ImageView[]{
				content.findViewById(R.id.familiar_followers_ava1),
				content.findViewById(R.id.familiar_followers_ava2),
				content.findViewById(R.id.familiar_followers_ava3),
		};
		familiarFollowersLabel=content.findViewById(R.id.familiar_followers_label);
		badgesLayout=content.findViewById(R.id.badges);
		joinDate=content.findViewById(R.id.join_date);
		fieldsLayout=content.findViewById(R.id.fields);
		expandFieldsButton=content.findViewById(R.id.expand_fields_button);

		avatar.setOutlineProvider(OutlineProviders.roundedRect(16));
		avatar.setClipToOutline(true);

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-V.dp(48);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[3];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.profile_tab0;
				case 1 -> R.id.profile_tab1;
				case 2 -> R.id.profile_tab2;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		pager.setOffscreenPageLimit(10);
		pager.setAdapter(new ProfilePagerAdapter());
		pager.getLayoutParams().height=getResources().getDisplayMetrics().heightPixels;

		scrollView.setScrollableChildSupplier(this::getScrollableRecyclerView);

		sizeWrapper.addView(content);

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabbar.setTabTextSize(V.dp(14));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, (tab, position)->{
			tab.setText(tabs.get(position).title);
			tab.view.textView.setSingleLine();
			tab.view.textView.setEllipsize(TextUtils.TruncateAt.END);
		});
		tabbar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				if(getFragmentForPage(tab.getPosition()) instanceof ScrollableToTop stt)
					stt.scrollToTop();
			}
		});

		cover.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setEmpty();
			}
		});

		actionButton.setOnClickListener(this::onActionButtonClick);
		avatar.setOnClickListener(this::onAvatarClick);
		cover.setOnClickListener(this::onCoverClick);
		refreshLayout.setOnRefreshListener(this);
		fab.setOnClickListener(this::onFabClick);
		familiarFollowersRow.setOnClickListener(this::onFamiliarFollowersClick);

		if(savedInstanceState!=null){
			featuredFragment=(ProfileFeaturedFragment) getChildFragmentManager().getFragment(savedInstanceState, "featured");
			timelineFragment=(AccountTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "timeline");
			mediaTimelineFragment=(AccountSimpleTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "media");
		}

		if(loaded){
			bindHeaderView();
			dataLoaded();
			tabLayoutMediator.attach();
		}else{
			fab.setVisibility(View.GONE);
		}

		followersBtn.setOnClickListener(this::onFollowersOrFollowingClick);
		followingBtn.setOnClickListener(this::onFollowersOrFollowingClick);

		username.setOnLongClickListener(v->{
			if(account==null)
				return true;
			String username=account.acct;
			if(!username.contains("@")){
				username+="@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
			}
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, "@"+username));
			UiUtils.maybeShowTextCopiedToast(getActivity());
			return true;
		});

		scrollableContent.setDrawingOrderCallback((count, pos)->{
			// The header is the first child, draw it last to overlap everything for the photo viewer transition to look nice
			if(pos==count-1)
				return 0;
			// Offset the order of other child views to compensate
			return pos+1;
		});

		int colorBackground=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background);
		int colorPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setProgressBackgroundColorSchemeColor(UiUtils.alphaBlendColors(colorBackground, colorPrimary, 0.11f));
		refreshLayout.setColorSchemeColors(colorPrimary);

		usernameHelp.setOnClickListener(v->{
			if(account==null)
				return;
			new DecentralizationExplainerSheet(getActivity(), accountID, account).show();
		});
		menuButton.setOnClickListener(v->{
			if(buttonMenu==null){
				buttonMenu=new PopupMenu(getActivity(), menuButton);
				buttonMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
				onCreateOptionsMenu(buttonMenu.getMenu(), buttonMenu.getMenuInflater());
			}
			onPrepareOptionsMenu(buttonMenu.getMenu());
			buttonMenu.show();
		});
		notificationsButton.setOnClickListener(v->{
			new SetAccountFollowed(account.id, true, relationship.showingReblogs, !relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
							new Snackbar.Builder(getActivity())
									.setText(result.notifying ? R.string.new_post_notifications_enabled : R.string.new_post_notifications_disabled)
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		});
		familiarFollowersRow.setVisibility(View.GONE);

		fieldsLayout.setColumnCount(getResources().getConfiguration().screenWidthDp>=640 ? 4 : 2);
		fieldsLayout.setLayoutCallback(()->{
			for(int i=0;i<fieldsLayout.getChildCount();i++){
				View field=fieldsLayout.getChildAt(i);
				if(field.getVisibility()==View.GONE)
					continue;
				FieldViewHolder vh=(FieldViewHolder) field.getTag();
				if(vh.value.getLayout().getLineWidth(0)>vh.value.getMeasuredWidth()){
					vh.valueEllipsis.setVisibility(View.VISIBLE);
					vh.value.setPaddingRelative(0, 0, V.dp(24), 0);
				}else{
					vh.valueEllipsis.setVisibility(View.INVISIBLE);
					vh.value.setPadding(0, 0, 0, 0);
				}
				if(vh.name.getLayout().getLineWidth(0)>vh.name.getMeasuredWidth()){
					vh.nameEllipsis.setVisibility(View.VISIBLE);
					vh.name.setPaddingRelative(0, 0, V.dp(24), 0);
				}else{
					vh.nameEllipsis.setVisibility(View.INVISIBLE);
					vh.name.setPadding(0, 0, 0, 0);
				}
			}
			expandFieldsButton.setVisibility(fieldsLayout.hasHiddenViews() ? View.VISIBLE : View.GONE);
		});
		expandFieldsButton.setOnClickListener(v->{
			UiUtils.beginLayoutTransition(contentView);
			fieldsLayout.setMaxRows(Integer.MAX_VALUE);
		});

		return sizeWrapper;
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		buttonMenu=null;
	}

	@Override
	protected void doLoadData(){
		currentRequest=new GetAccountByID(profileAccountID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Account result){
						account=result;
						isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
						bindHeaderView();
						dataLoaded();
						if(!tabLayoutMediator.isAttached())
							tabLayoutMediator.attach();
						if(!isOwnProfile)
							loadRelationship();
						else
							AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						if(refreshing){
							refreshing=false;
							refreshLayout.setRefreshing(false);
							if(timelineFragment.loaded)
								timelineFragment.onRefresh();
							if(featuredFragment.loaded)
								featuredFragment.onRefresh();
							if(mediaTimelineFragment!=null && mediaTimelineFragment.loaded)
								mediaTimelineFragment.onRefresh();
						}
						V.setVisibilityAnimated(fab, View.VISIBLE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onRefresh(){
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
		if(isOwnProfile){
			AccountSessionManager.get(accountID).updateAccountInfo();
		}
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(account));
		args.putBoolean("__is_tab", true);
		args.putBoolean("noAutoLoad", true);
		if(featuredFragment==null){
			featuredFragment=new ProfileFeaturedFragment();
			featuredFragment.setArguments(args);
		}
		if(timelineFragment==null){
			timelineFragment=AccountTimelineFragment.newInstance(accountID, account, true);
			mediaTimelineFragment=AccountSimpleTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.MEDIA, false);
		}
		if(!refreshing){
			tabs.clear();
			tabs.add(new Tab(timelineFragment, getString(R.string.profile_timeline)));
			tabs.add(new Tab(mediaTimelineFragment, getString(R.string.media)));
			tabs.add(new Tab(featuredFragment, getString(R.string.profile_featured)));
			pager.getAdapter().notifyDataSetChanged();
		}
		super.dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		// To avoid the callback triggering on first layout with position=0 before anything is instantiated
		pager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				pager.getViewTreeObserver().removeOnPreDrawListener(this);
				pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
					@Override
					public void onPageSelected(int position){
						Fragment _page=getFragmentForPage(position);
						if(_page instanceof BaseRecyclerFragment<?> page && page.isAdded()){
							if(!page.loaded && !page.isDataLoading())
								page.loadData();
						}
					}

					@Override
					public void onPageScrollStateChanged(int state){
						refreshLayout.setEnabled(state!=ViewPager2.SCROLL_STATE_DRAGGING);
					}
				});
				return true;
			}
		});

		tabsColorBackground=((LayerDrawable)tabbar.getBackground()).findDrawableByLayerId(R.id.color_overlay);

		onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, getToolbar());
		scrollView.setOnScrollChangeListener(this::onScrollChanged);
		if(!loaded)
			bindHeaderViewForPreviewMaybe();
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(featuredFragment==null)
			return;
		if(featuredFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "featured", featuredFragment);
		if(timelineFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "timeline", timelineFragment);
		if(mediaTimelineFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "media", mediaTimelineFragment);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
		fieldsLayout.setColumnCount(newConfig.screenWidthDp>=640 ? 4 : 2);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(contentView!=null){
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+insetBottom;
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}else{
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16);
			}
		}
		super.onApplyWindowInsets(insets);
	}

	@Subscribe
	public void onAccountUpdated(SelfAccountUpdatedEvent ev){
		if(ev.accountID().equals(accountID) && account!=null && account.id.equals(ev.account().id)){
			account=ev.account();
			bindHeaderView();
		}
	}

	private void applyChildWindowInsets(){
		if(timelineFragment!=null && timelineFragment.isAdded() && childInsets!=null){
			for(Tab t:tabs){
				if(t.fragment instanceof WindowInsetsAwareFragment waf)
					waf.onApplyWindowInsets(childInsets);
			}
		}
	}

	private void bindHeaderViewForPreviewMaybe(){
		if(loaded)
			return;
		String username=getArguments().getString("accountUsername");
		String domain=getArguments().getString("accountDomain");
		if(TextUtils.isEmpty(username) || TextUtils.isEmpty(domain))
			return;
		content.setVisibility(View.VISIBLE);
		progress.setVisibility(View.GONE);
		errorView.setVisibility(View.GONE);
		innerProgress.setVisibility(View.VISIBLE);
		this.username.setText("@"+username+"@"+domain);
		name.setText(username);
		avatar.setImageResource(R.drawable.image_placeholder);
		cover.setImageResource(R.drawable.image_placeholder);
		actions.setVisibility(View.GONE);
		countersLayout.setVisibility(View.GONE);
		tabsDivider.setVisibility(View.GONE);
	}

	private TextView makeBadge(String text, int iconRes, int bgRes, boolean bgIsColorAttr, int textColorRes, boolean tintIcon){
		TextView badge=new TextView(getActivity());
		badge.setText(text);
		if(bgIsColorAttr)
			badge.setBackgroundColor(UiUtils.getThemeColor(getActivity(), bgRes));
		else
			badge.setBackgroundResource(bgRes);
		int textColor=UiUtils.getThemeColor(getActivity(), textColorRes);
		badge.setTextColor(textColor);
		badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
		badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
		Drawable icon=getResources().getDrawable(iconRes, getActivity().getTheme());
		icon.setBounds(0, 0, V.dp(16), V.dp(16));
		if(tintIcon){
			icon=icon.mutate();
			icon.setTint(textColor);
		}
		badge.setCompoundDrawablesRelative(icon, null, null, null);
		badge.setCompoundDrawablePadding(V.dp(2));
		badge.setPadding(V.dp(4), 0, V.dp(4), 0);
		badge.setOutlineProvider(OutlineProviders.roundedRect(8));
		badge.setClipToOutline(true);
		badge.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(24)));
		badge.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
		return badge;
	}

	private void bindHeaderView(){
		if(innerProgress.getVisibility()==View.VISIBLE){
			TransitionManager.beginDelayedTransition(contentView, new TransitionSet()
					.addTransition(new Fade(Fade.IN | Fade.OUT))
					.excludeChildren(actions, true)
					.setDuration(250)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
			);
			innerProgress.setVisibility(View.GONE);
			countersLayout.setVisibility(View.VISIBLE);
			actions.setVisibility(View.VISIBLE);
			tabsDivider.setVisibility(View.VISIBLE);
		}
		setTitle(account.displayName);
		ViewImageLoader.loadWithoutAnimation(avatar, avatar.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(100), V.dp(100)));
		ViewImageLoader.loadWithoutAnimation(cover, cover.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
		SpannableStringBuilder ssb=new SpannableStringBuilder(account.displayName);
		if(GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
		name.setText(ssb);
		setTitle(ssb);

		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);
		String domain=account.getDomain();
		if(TextUtils.isEmpty(domain))
			domain=AccountSessionManager.get(accountID).domain;

		if(account.locked){
			ssb=new SpannableStringBuilder("@");
			ssb.append(account.username);
			ssb.append("@");
			ssb.append(domain);
			ssb.append(" ");
			Drawable lock=username.getResources().getDrawable(R.drawable.ic_lock_fill1_20px, getActivity().getTheme()).mutate();
			lock.setBounds(0, 0, V.dp(16), V.dp(16));
			lock.setTint(username.getCurrentTextColor());
			ssb.append(getString(R.string.manually_approves_followers), new ImageSpanThatDoesNotBreakShitForNoGoodReason(lock, ImageSpan.ALIGN_BOTTOM), 0);
			username.setText(ssb);
		}else{
			username.setText("@"+account.username+"@"+domain);
		}

		CharSequence parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID, account, getActivity());
		if(TextUtils.isEmpty(parsedBio)){
			bio.setVisibility(View.GONE);
		}else{
			bio.setVisibility(View.VISIBLE);
			bio.setText(parsedBio);
		}
		followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
		followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
		postsCount.setText(UiUtils.abbreviateNumber(account.statusesCount));

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		notificationsButton.setVisibility(View.GONE);
		if(isSelf){
			actionButton.setText(R.string.edit_profile);
			TypedArray ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Outlined_Neutral, new int[]{android.R.attr.background});
			actionButton.setBackground(ta.getDrawable(0));
			ta.recycle();
			ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Outlined_Neutral, new int[]{android.R.attr.textColor});
			actionButton.setTextColor(ta.getColorStateList(0));
			ta.recycle();
		}else{
			actionButton.setVisibility(View.GONE);
		}

		updateHeaderBadges();

		LocalDateTime joinDateValue=LocalDateTime.ofInstant(account.createdAt, ZoneId.systemDefault());
		String joinDateText;
		if(joinDateValue.getYear()==LocalDateTime.now().getYear())
			joinDateText=DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "d MMM")).format(joinDateValue);
		else
			joinDateText=DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy")).format(joinDateValue);
		joinDate.setText(joinDateText);

		fields.clear();

		fieldsLayout.removeAllViews();
		expandFieldsButton.setVisibility(View.GONE);
		if(account.fields.isEmpty()){
			fieldsLayout.setVisibility(View.GONE);
		}else{
			fieldsLayout.setVisibility(View.VISIBLE);
			for(AccountField field:account.fields){
				View fv=getActivity().getLayoutInflater().inflate(R.layout.item_profile_about, fieldsLayout, false);
				TextView nameView=fv.findViewById(R.id.title);
				TextView valueView=fv.findViewById(R.id.value);
				View verifiedIcon=fv.findViewById(R.id.verified_icon);

				SpannableStringBuilder name=HtmlParser.parseCustomEmoji(field.name, account.emojis);
				nameView.setText(name);
				SpannableStringBuilder value=HtmlParser.parse(field.value, account.emojis, List.of(), List.of(), accountID, account, getActivity(), false);
				valueView.setText(value);

				UiUtils.loadCustomEmojiInTextView(nameView);
				UiUtils.loadCustomEmojiInTextView(valueView);

				if(field.verifiedAt!=null){
					fv.setBackgroundColor(UiUtils.isDarkTheme() ? 0xff032E15 : 0xffF0FDF4);
					fv.setOutlineProvider(OutlineProviders.roundedRect(8));
					fv.setClipToOutline(true);
				}else{
					verifiedIcon.setVisibility(View.GONE);
				}

				FieldViewHolder fvh=new FieldViewHolder();
				fvh.name=nameView;
				fvh.value=valueView;
				fvh.nameEllipsis=fv.findViewById(R.id.title_ellipsis);
				fvh.valueEllipsis=fv.findViewById(R.id.value_ellipsis);
				fv.setTag(fvh);

				View.OnClickListener expandListener=v->new LongProfileFieldSheet(getActivity(), name, value).show();
				fvh.nameEllipsis.setOnClickListener(expandListener);
				fvh.valueEllipsis.setOnClickListener(expandListener);

				fieldsLayout.addView(fv);
			}
		}
	}

	private void updateHeaderBadges(){
		badgesLayout.removeAllViews();

		if(relationship!=null){
			if(relationship.blocking)
				badgesLayout.addView(makeBadge(getString(R.string.button_blocked), R.drawable.ic_block_20px, R.attr.colorM3Error, true, R.attr.colorM3OnError, true));
			if(relationship.muting)
				badgesLayout.addView(makeBadge(getString(R.string.user_is_muted), R.drawable.ic_badge_muted, R.drawable.bg_m3_surface1_inverse, false, R.attr.colorM3OnSurfaceInverse, true));
		}

		if(account.roles!=null){
			for(Account.PublicRole role:account.roles){
				badgesLayout.addView(makeBadge(role.name, R.drawable.ic_badge_admin, R.drawable.bg_m3_surface1, false, R.attr.colorM3OnSurface, false));
			}
		}

		if(account.bot)
			badgesLayout.addView(makeBadge(getString(R.string.user_is_bot), R.drawable.ic_badge_bot, R.drawable.bg_m3_surface1, false, R.attr.colorM3OnSurface, true));

		if(badgesLayout.getChildCount()==0)
			badgesLayout.setVisibility(View.GONE);
		else
			badgesLayout.setVisibility(View.VISIBLE);
	}

	private void updateToolbar(){
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
		if(onScrollListener!=null){
			onScrollListener.setViews(getToolbar());
		}
		getToolbar().setTranslationZ(tabBarIsAtTop ? 0 : V.dp(3));
	}

	private CharSequence makeRedString(CharSequence s){
		int color=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error);
		SpannableString ss=new SpannableString(s);
		ss.setSpan(new ForegroundColorSpan(color), 0, ss.length(), 0);
		return ss;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(relationship==null && !isOwnProfile)
			return;
		inflater.inflate(isOwnProfile ? R.menu.profile_own : R.menu.profile, menu);
		onPrepareOptionsMenu(menu); // TODO make appkit call this
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()){
			menu.setGroupDividerEnabled(true);
		}

		if(isOwnProfile || relationship==null)
			return;
		if(relationship.followedBy)
			menu.findItem(R.id.remove_follower).setTitle(makeRedString(getString(R.string.remove_follower)));
		else
			menu.findItem(R.id.remove_follower).setVisible(false);
		menu.findItem(R.id.mute).setTitle(getString(relationship.muting ? R.string.unmute_account : R.string.mute_account, account.getDisplayUsername()));
		menu.findItem(R.id.block).setTitle(makeRedString(getString(relationship.blocking ? R.string.unblock_account : R.string.block_account)));
		menu.findItem(R.id.report).setTitle(makeRedString(getString(R.string.report_account)));
		if(relationship.following){
			MenuItem hideBoosts=menu.findItem(R.id.hide_boosts);
			hideBoosts.setVisible(true);
			hideBoosts.setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user));
			MenuItem feature=menu.findItem(R.id.feature);
			feature.setVisible(true);
			feature.setTitle(getString(relationship.endorsed ? R.string.unfeature_user : R.string.feature_user));
		}else{
			menu.findItem(R.id.hide_boosts).setVisible(false);
			menu.findItem(R.id.feature).setVisible(false);
		}
		if(!account.isLocal())
			menu.findItem(R.id.block_domain).setTitle(makeRedString(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain())));
		else
			menu.findItem(R.id.block_domain).setVisible(false);
		menu.findItem(R.id.add_to_list).setVisible(relationship.following);
		menu.findItem(R.id.personal_note).setTitle(TextUtils.isEmpty(relationship.note) ? R.string.add_user_personal_note : R.string.edit_user_personal_note);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.share){
			UiUtils.openSystemShareSheet(getActivity(), account);
		}else if(id==R.id.mute){
			confirmToggleMuted();
		}else if(id==R.id.block){
			confirmToggleBlocked();
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			args.putParcelable("relationship", Parcels.wrap(relationship));
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account, relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship();
			}, this::updateRelationship);
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs, relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.add_to_list){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			Nav.go(getActivity(), AddAccountToListsFragment.class, args);
		}else if(id==R.id.copy_link){
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, account.url));
			UiUtils.maybeShowTextCopiedToast(getActivity());
		}else if(id==R.id.qr_code){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			ProfileQrCodeFragment qf=new ProfileQrCodeFragment();
			qf.setArguments(args);
			qf.show(getChildFragmentManager(), "qrDialog");
		}else if(id==R.id.favorites){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("isFavorites", true);
			Nav.go(getActivity(), SavedPostsTimelineFragment.class, args);
		}else if(id==R.id.bookmarks){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("isFavorites", false);
			Nav.go(getActivity(), SavedPostsTimelineFragment.class, args);
		}else if(id==R.id.followed_hashtags){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), ManageFollowedHashtagsFragment.class, args);
		}else if(id==R.id.account_settings){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), SettingsAccountFragment.class, args);
		}else if(id==R.id.remove_follower){
			AlertDialog alert=new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.confirm_remove_follower_title)
					.setMessage(getString(R.string.confirm_remove_follower, account.getDisplayUsername()))
					.setPositiveButton(R.string.remove_follower, null)
					.setNegativeButton(R.string.cancel, null)
					.show();
			Button okButton=alert.getButton(AlertDialog.BUTTON_POSITIVE);
			okButton.setOnClickListener(v->{
				UiUtils.showProgressForAlertButton(okButton, true);
				new RemoveAccountFromFollowers(account.id)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								updateRelationship(result);
								UiUtils.showProgressForAlertButton(okButton, false);
								alert.dismiss();
							}

							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null)
									return;
								error.showToast(getActivity());
								UiUtils.showProgressForAlertButton(okButton, false);
							}
						})
						.wrapProgress(getActivity(), R.string.loading, true)
						.exec(accountID);
			});
		}else if(id==R.id.personal_note){
			AlertDialog.Builder bldr=new M3AlertDialogBuilder(getActivity())
					.setHelpText(R.string.user_personal_note_explanation)
					.setTitle(TextUtils.isEmpty(relationship.note) ? R.string.add_user_personal_note : R.string.edit_user_personal_note)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.save, null);

			FloatingHintEditTextLayout editWrap=(FloatingHintEditTextLayout) bldr.getContext().getSystemService(LayoutInflater.class).inflate(R.layout.floating_hint_edit_text, null);
			EditText edit=editWrap.findViewById(R.id.edit);
			edit.setHint(R.string.user_personal_note);
			edit.setSingleLine(false);
			edit.setMaxLines(5);
			edit.setMinLines(2);
			edit.setGravity(Gravity.TOP | Gravity.START);
			if(!TextUtils.isEmpty(relationship.note))
				edit.setText(relationship.note);
			editWrap.updateHint();
			bldr.setView(editWrap);
			AlertDialog alert=bldr.show();
			Button saveButton=alert.getButton(AlertDialog.BUTTON_POSITIVE);
			saveButton.setOnClickListener(v->{
				UiUtils.showProgressForAlertButton(saveButton, true);
				new SetAccountPersonalNote(account.id, edit.getText().toString())
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								updateRelationship(result);
								UiUtils.showProgressForAlertButton(saveButton, false);
								alert.dismiss();
							}

							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null)
									return;
								error.showToast(getActivity());
								UiUtils.showProgressForAlertButton(saveButton, false);
							}
						})
						.exec(accountID);
			});
		}else if(id==R.id.feature){
			new SetAccountEndorsed(account.id, !relationship.endorsed)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							if(getActivity()==null)
								return;
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, true)
					.exec(accountID);
		}
		return true;
	}

	private void loadRelationship(){
		MastodonAPIRequest<List<Relationship>> relReq=new GetAccountRelationships(Collections.singletonList(account.id));
		relReq.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						relationshipRequests.remove(relReq);
						if(getActivity()==null)
							return;
						if(!result.isEmpty()){
							relationship=result.get(0);
							updateRelationship();
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipRequests.remove(relReq);
					}
				})
				.exec(accountID);
		MastodonAPIRequest<List<FamiliarFollowers>> followersReq=new GetAccountFamiliarFollowers(Set.of(account.id));
		followersReq.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<FamiliarFollowers> result){
						relationshipRequests.remove(followersReq);
						if(getActivity()==null)
							return;
						for(FamiliarFollowers ff:result){
							if(ff.id.equals(account.id)){
								familiarFollowers=ff.accounts;
								updateFamiliarFollowers();
								break;
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipRequests.remove(followersReq);
					}
				})
				.exec(accountID);
		relationshipRequests.add(relReq);
		relationshipRequests.add(followersReq);
	}

	private void setRelationshipToActionButton(Relationship relationship, Button button){
		// TODO move this to UiUtils if/when we change the styles of these buttons in the rest of the app
		int styleRes;
		if(relationship.blocking){
			button.setText(R.string.unblock);
			styleRes=R.style.Widget_Mastodon_M3_Button_Outlined_Neutral;
		}else if(!relationship.following && !relationship.followedBy && !relationship.requested){
			button.setText(R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else if(!relationship.following && !relationship.requested && account.locked){
			button.setText(R.string.request_to_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else if(!relationship.following && relationship.requested){
			button.setText(R.string.cancel_follow_request);
			styleRes=R.style.Widget_Mastodon_M3_Button_Outlined_Neutral;
		}else if(!relationship.following && relationship.followedBy){
			button.setText(R.string.follow_back);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else{
			button.setText(R.string.unfollow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Outlined_Neutral;
		}

		button.setEnabled(!relationship.blockedBy);
		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	private void updateRelationship(){
		invalidateOptionsMenu();
		actionButton.setVisibility(View.VISIBLE);
		setRelationshipToActionButton(relationship, actionButton);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		updateHeaderBadges();
		if(relationship.following){
			notificationsButton.setVisibility(View.VISIBLE);
			notificationsButton.setImageResource(relationship.notifying ? R.drawable.ic_notifications_fill1_24px : R.drawable.ic_notifications_24px);
			notificationsButton.setContentDescription(getString(relationship.notifying ? R.string.disable_new_post_notifications : R.string.enable_new_post_notifications, account.getDisplayUsername()));
		}else{
			notificationsButton.setVisibility(View.GONE);
		}
	}

	private void updateFamiliarFollowers(){
		if(!familiarFollowers.isEmpty()){
			familiarFollowersRow.setVisibility(View.VISIBLE);
			List<AccountViewModel> followers=familiarFollowers.stream().limit(3).map(a->new AccountViewModel(a, accountID, false, getActivity())).collect(Collectors.toList());
			String template=switch(familiarFollowers.size()){
				case 1 -> getString(R.string.familiar_followers_one, "{first}");
				case 2 -> getString(R.string.familiar_followers_two, "{first}", "{second}");
				default -> getResources().getQuantityString(R.plurals.familiar_followers_many, familiarFollowers.size()-2, "{first}", "{second}", familiarFollowers.size()-2);
			};
			SpannableStringBuilder ssb=new SpannableStringBuilder(template);
			if(familiarFollowers.size()>1){
				int index=template.indexOf("{second}");
				ssb.replace(index, index+8, followers.get(1).parsedName);
				ssb.setSpan(new TypefaceSpan("sans-serif-medium"), index, index+followers.get(1).parsedName.length(), 0);
				template=template.replace("{second}", "#".repeat(followers.get(1).parsedName.length()));
			}
			int index=template.indexOf("{first}");
			ssb.replace(index, index+7, followers.get(0).parsedName);
			ssb.setSpan(new TypefaceSpan("sans-serif-medium"), index, index+followers.get(0).parsedName.length(), 0);
			familiarFollowersLabel.setText(ssb);
			UiUtils.loadCustomEmojiInTextView(familiarFollowersLabel);
			if(familiarFollowers.size()<3)
				familiarFollowersAvatars[2].setVisibility(View.GONE);
			if(familiarFollowers.size()<2)
				familiarFollowersAvatars[1].setVisibility(View.GONE);

			int i=0;
			for(AccountViewModel avm:followers){
				ViewImageLoader.loadWithoutAnimation(familiarFollowersAvatars[i], getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), avm.avaRequest);
				i++;
			}
		}
	}

	private void syncScrollState(){
		scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				scrollView.getViewTreeObserver().removeOnPreDrawListener(this);
				onScrollChanged(scrollView, 0, scrollView.getScrollY(), 0, -1, true);
				return true;
			}
		});
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		onScrollChanged(v, scrollX, scrollY, oldScrollX, oldScrollY, false);
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY, boolean firstCallback){
		if(scrollY>cover.getHeight()){
			cover.setTranslationY(scrollY-(cover.getHeight()));
			cover.setTranslationZ(V.dp(10));
			cover.setTransform(cover.getHeight()/2f);
		}else{
			cover.setTranslationY(0f);
			cover.setTranslationZ(0f);
			cover.setTransform(scrollY/2f);
		}
		cover.invalidate();
		if(currentPhotoViewer!=null){
			currentPhotoViewer.offsetView(0, oldScrollY-scrollY);
		}
		onScrollListener.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY);

		boolean newTabBarIsAtTop=!scrollView.canScrollVertically(1);
		if(newTabBarIsAtTop!=tabBarIsAtTop || firstCallback){
			tabBarIsAtTop=newTabBarIsAtTop;

			if(tabBarIsAtTop){
				// ScrollView would sometimes leave 1 pixel unscrolled, force it into the correct scrollY
				int maxY=scrollView.getChildAt(0).getHeight()-scrollView.getHeight();
				if(scrollView.getScrollY()!=maxY)
					scrollView.scrollTo(0, maxY);
			}

			if(tabBarColorAnim!=null)
				tabBarColorAnim.cancel();
			if(firstCallback){
				tabsColorBackground.setAlpha(tabBarIsAtTop ? 20 : 0);
				tabbar.setTranslationZ(tabBarIsAtTop ? V.dp(3) : 0);
				getToolbar().setTranslationZ(tabBarIsAtTop || scrollY==0 ? 0 : V.dp(3));
				tabsDivider.setAlpha(tabBarIsAtTop ? 0 : 1);
			}else{
				AnimatorSet set=new AnimatorSet();
				set.playTogether(
						ObjectAnimator.ofInt(tabsColorBackground, "alpha", tabBarIsAtTop ? 20 : 0),
						ObjectAnimator.ofFloat(tabbar, View.TRANSLATION_Z, tabBarIsAtTop ? V.dp(3) : 0),
						ObjectAnimator.ofFloat(getToolbar(), View.TRANSLATION_Z, tabBarIsAtTop ? 0 : V.dp(3)),
						ObjectAnimator.ofFloat(tabsDivider, View.ALPHA, tabBarIsAtTop ? 0 : 1)
				);
				set.setDuration(150);
				set.setInterpolator(CubicBezierInterpolator.DEFAULT);
				set.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						tabBarColorAnim=null;
					}
				});
				tabBarColorAnim=set;
				set.start();
			}
			setHasOptionsMenu(tabBarIsAtTop);
		}
		if((scrollY==0 && oldScrollY!=0) || (scrollY!=0 && oldScrollY==0)){
			refreshLayout.setEnabled(scrollY==0);
		}
	}

	private Fragment getFragmentForPage(int page){
		return tabs.get(page).fragment;
	}

	private RecyclerView getScrollableRecyclerView(){
		return getFragmentForPage(pager.getCurrentItem()).getView().findViewById(R.id.list);
	}

	private void onActionButtonClick(View v){
		if(isOwnProfile){
			Bundle extras=new Bundle();
			extras.putString("account", accountID);
			extras.putInt("featuredTagCount", timelineFragment.getFeaturedHashtagCount());
			Nav.go(getActivity(), ProfileEditFragment.class, extras);
		}else{
			UiUtils.performAccountAction(getActivity(), account, accountID, relationship, actionButton, this::setActionProgressVisible, this::updateRelationship);
		}
	}

	private void setActionProgressVisible(boolean visible){
		actionButton.setTextVisible(!visible);
		actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible)
			actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		actionButton.setClickable(!visible);
	}

	private void confirmToggleMuted(){
		UiUtils.confirmToggleMuteUser(getActivity(), accountID, account, relationship.muting, this::updateRelationship);
	}

	private void confirmToggleBlocked(){
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
	}

	private void updateRelationship(Relationship r){
		relationship=r;
		updateRelationship();
	}

	private List<Attachment> createFakeAttachments(String url, Drawable drawable){
		Attachment att=new Attachment();
		att.type=Attachment.Type.IMAGE;
		att.url=url;
		att.meta=new Attachment.Metadata();
		att.meta.width=drawable.getIntrinsicWidth();
		att.meta.height=drawable.getIntrinsicHeight();
		return Collections.singletonList(att);
	}

	private void onAvatarClick(View v){
		if(account==null)
			return;
		Drawable ava=avatar.getDrawable();
		if(ava==null)
			return;
		int radius=V.dp(16);
		currentPhotoViewer=new PhotoViewer(getActivity(), null, createFakeAttachments(account.avatar, ava), 0,
				null, accountID, new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->currentPhotoViewer=null, ()->ava, null, null));
	}

	private void onCoverClick(View v){
		if(account==null)
			return;
		Drawable drawable=cover.getDrawable();
		if(drawable==null || drawable instanceof ColorDrawable || account.headerStatic.endsWith("/missing.png"))
			return;
		currentPhotoViewer=new PhotoViewer(getActivity(), null, createFakeAttachments(account.header, drawable), 0,
				null, accountID, new SingleImagePhotoViewerListener(cover, cover, null, this, ()->currentPhotoViewer=null, ()->drawable, ()->avatarBorder.setTranslationZ(2), ()->avatarBorder.setTranslationZ(0)));
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(!AccountSessionManager.getInstance().isSelf(accountID, account)){
			args.putString("prefilledText", '@'+account.acct+' ');
		}
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void onFamiliarFollowersClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		args.putInt("count", familiarFollowers.size());
		Nav.go(getActivity(), FamiliarFollowerListFragment.class, args);
	}

	@Override
	public void scrollToTop(){
		getScrollableRecyclerView().scrollToPosition(0);
		scrollView.smoothScrollTo(0, 0);
	}

	private void onFollowersOrFollowingClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		Class<? extends Fragment> cls;
		if(v.getId()==R.id.followers_btn)
			cls=FollowerListFragment.class;
		else if(v.getId()==R.id.following_btn)
			cls=FollowingListFragment.class;
		else
			return;
		Nav.go(getActivity(), cls, args);
	}

	private boolean isActionButtonInView(){
		return actionButton.getVisibility()==View.VISIBLE && actionButtonWrap.getTop()+actionButtonWrap.getHeight()>scrollView.getScrollY();
	}

	@Override
	public void onProvideAssistContent(AssistContent content){
		if(account!=null){
			content.setWebUri(Uri.parse(account.url));
		}
	}

	private class ProfilePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=new FrameLayout(parent.getContext());
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			FrameLayout fragmentView=tabViews[position];
			fragmentView.setVisibility(View.VISIBLE);
			if(fragmentView.getParent() instanceof ViewGroup parent)
				parent.removeView(fragmentView);
			((FrameLayout)holder.itemView).addView(fragmentView);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(fragmentView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						getChildFragmentManager().executePendingTransactions();
						if(fragment.isAdded()){
							holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
							applyChildWindowInsets();
						}
						return true;
					}
				});
			}
		}

		@Override
		public int getItemCount(){
			return loaded ? tabs.size() : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}

	private static class FieldViewHolder{
		public TextView name, value;
		public ImageButton nameEllipsis, valueEllipsis;
	}

	private static class Tab{
		public Fragment fragment;
		public String title;

		public Tab(Fragment fragment, String title){
			this.fragment=fragment;
			this.title=title;
		}
	}
}
