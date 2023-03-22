package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.CustomDrawingOrderLinearLayout;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class ProfileFragment extends LoaderFragment implements OnBackPressedListener, ScrollableToTop{
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder;
	private TextView name, username, bio, followersCount, followersLabel, followingCount, followingLabel;
	private ProgressBarButton actionButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private ProfileFeaturedFragment featuredFragment;
	private AccountTimelineFragment timelineFragment;
	private ProfileAboutFragment aboutFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private View followersBtn, followingBtn;
	private EditText nameEdit, bioEdit;
	private ProgressBar actionProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private TextView followsYouView;
	private LinearLayout countersLayout;
	private View nameEditWrap, bioEditWrap;
	private View tabsDivider;
	private View actionButtonWrap;
	private CustomDrawingOrderLinearLayout scrollableContent;

	private Account account;
	private String accountID;
	private Relationship relationship;
	private boolean isOwnProfile;
	private ArrayList<AccountField> fields=new ArrayList<>();

	private boolean isInEditMode;
	private Uri editNewAvatar, editNewCover;
	private String profileAccountID;
	private boolean refreshing;
	private View fab;
	private WindowInsets childInsets;
	private PhotoViewer currentPhotoViewer;
	private boolean editModeLoading;
	private ElevationOnScrollListener onScrollListener;
	private Drawable tabsColorBackground;
	private boolean tabBarIsAtTop;
	private Animator tabBarColorAnim;
	private MenuItem editSaveMenuItem;

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
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile, container, false);

		avatar=content.findViewById(R.id.avatar);
		cover=content.findViewById(R.id.cover);
		avatarBorder=content.findViewById(R.id.avatar_border);
		name=content.findViewById(R.id.name);
		username=content.findViewById(R.id.username);
		bio=content.findViewById(R.id.bio);
		followersCount=content.findViewById(R.id.followers_count);
		followersLabel=content.findViewById(R.id.followers_label);
		followersBtn=content.findViewById(R.id.followers_btn);
		followingCount=content.findViewById(R.id.following_count);
		followingLabel=content.findViewById(R.id.following_label);
		followingBtn=content.findViewById(R.id.following_btn);
		actionButton=content.findViewById(R.id.profile_action_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		nameEdit=content.findViewById(R.id.name_edit);
		bioEdit=content.findViewById(R.id.bio_edit);
		nameEditWrap=content.findViewById(R.id.name_edit_wrap);
		bioEditWrap=content.findViewById(R.id.bio_edit_wrap);
		actionProgress=content.findViewById(R.id.action_progress);
		fab=content.findViewById(R.id.fab);
		followsYouView=content.findViewById(R.id.follows_you);
		countersLayout=content.findViewById(R.id.profile_counters);
		tabsDivider=content.findViewById(R.id.tabs_divider);
		actionButtonWrap=content.findViewById(R.id.profile_action_btn_wrap);
		scrollableContent=content.findViewById(R.id.scrollable_content);

		avatar.setOutlineProvider(OutlineProviders.roundedRect(24));
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
				case 0 -> R.id.profile_featured;
				case 1 -> R.id.profile_timeline;
				case 2 -> R.id.profile_about;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		pager.setOffscreenPageLimit(4);
		pager.setAdapter(new ProfilePagerAdapter());
		pager.getLayoutParams().height=getResources().getDisplayMetrics().heightPixels;

		scrollView.setScrollableChildSupplier(this::getScrollableRecyclerView);

		sizeWrapper.addView(content);

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabbar.setTabTextSize(V.dp(16));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.profile_featured;
					case 1 -> R.string.profile_timeline;
					case 2 -> R.string.profile_about;
					default -> throw new IllegalStateException();
				});
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
			String username=account.acct;
			if(!username.contains("@")){
				username+="@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
			}
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, "@"+username));
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU || UiUtils.isMIUI()){ // Android 13+ SystemUI shows its own thing when you put things into the clipboard
				Toast.makeText(getActivity(), R.string.text_copied, Toast.LENGTH_SHORT).show();
			}
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

		return sizeWrapper;
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
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		if(featuredFragment==null){
			featuredFragment=new ProfileFeaturedFragment();
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(account));
			args.putBoolean("__is_tab", true);
			featuredFragment.setArguments(args);
			timelineFragment=AccountTimelineFragment.newInstance(accountID, account, true);
			aboutFragment=new ProfileAboutFragment();
			aboutFragment.setFields(fields);
		}
		pager.getAdapter().notifyDataSetChanged();
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
						if(position==0)
							return;
						Fragment _page=getFragmentForPage(position);
						if(_page instanceof BaseRecyclerFragment<?> page){
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
		scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				scrollView.getViewTreeObserver().removeOnPreDrawListener(this);

				tabBarIsAtTop=!scrollView.canScrollVertically(1) && scrollView.getHeight()>0;
				tabsColorBackground.setAlpha(tabBarIsAtTop ? 20 : 0);
				tabbar.setTranslationZ(tabBarIsAtTop ? V.dp(3) : 0);
				tabsDivider.setAlpha(tabBarIsAtTop ? 0 : 1);

				return true;
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
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

	private void applyChildWindowInsets(){
		if(timelineFragment!=null && timelineFragment.isAdded() && childInsets!=null){
			timelineFragment.onApplyWindowInsets(childInsets);
			featuredFragment.onApplyWindowInsets(childInsets);
		}
	}

	private void bindHeaderView(){
		setTitle(account.displayName);
		setSubtitle(getResources().getQuantityString(R.plurals.x_posts, (int)(account.statusesCount%1000), account.statusesCount));
		ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(100), V.dp(100)));
		ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
		SpannableStringBuilder ssb=new SpannableStringBuilder(account.displayName);
		HtmlParser.parseCustomEmoji(ssb, account.emojis);
		name.setText(ssb);
		setTitle(ssb);

		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);

		if(account.locked){
			ssb=new SpannableStringBuilder("@");
			ssb.append(account.acct);
			if(isSelf){
				ssb.append('@');
				ssb.append(AccountSessionManager.getInstance().getAccount(accountID).domain);
			}
			ssb.append(" ");
			Drawable lock=username.getResources().getDrawable(R.drawable.ic_fluent_lock_closed_20_filled, getActivity().getTheme()).mutate();
			lock.setBounds(0, 0, lock.getIntrinsicWidth(), lock.getIntrinsicHeight());
			lock.setTint(username.getCurrentTextColor());
			ssb.append(getString(R.string.manually_approves_followers), new ImageSpan(lock, ImageSpan.ALIGN_BOTTOM), 0);
			username.setText(ssb);
		}else{
			// noinspection SetTextI18n
			username.setText('@'+account.acct+(isSelf ? ('@'+AccountSessionManager.getInstance().getAccount(accountID).domain) : ""));
		}
		CharSequence parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
		if(TextUtils.isEmpty(parsedBio)){
			bio.setVisibility(View.GONE);
		}else{
			bio.setVisibility(View.VISIBLE);
			bio.setText(parsedBio);
		}
		followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
		followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
		followersLabel.setText(getResources().getQuantityString(R.plurals.followers, (int)Math.min(999, account.followersCount)));
		followingLabel.setText(getResources().getQuantityString(R.plurals.following, (int)Math.min(999, account.followingCount)));

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		if(AccountSessionManager.getInstance().isSelf(accountID, account)){
			actionButton.setText(R.string.edit_profile);
			TypedArray ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.background});
			actionButton.setBackground(ta.getDrawable(0));
			ta.recycle();
			ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.textColor});
			actionButton.setTextColor(ta.getColorStateList(0));
			ta.recycle();
		}else{
			actionButton.setVisibility(View.GONE);
		}

		fields.clear();

		AccountField joined=new AccountField();
		joined.parsedName=joined.name=getString(R.string.profile_joined);
		joined.parsedValue=joined.value=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(account.createdAt, ZoneId.systemDefault()));
		fields.add(joined);

		for(AccountField field:account.fields){
			field.parsedValue=ssb=HtmlParser.parse(field.value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
			field.valueEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			ssb=new SpannableStringBuilder(field.name);
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
			field.parsedName=ssb;
			field.nameEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			field.emojiRequests=new ArrayList<>(field.nameEmojis.length+field.valueEmojis.length);
			for(CustomEmojiSpan span:field.nameEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			for(CustomEmojiSpan span:field.valueEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			fields.add(field);
		}

		if(aboutFragment!=null){
			aboutFragment.setFields(fields);
		}
	}

	private void updateToolbar(){
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
		UiUtils.setToolbarWithSubtitleAppearance(getToolbar());
		if(onScrollListener!=null){
			onScrollListener.setViews(getToolbar());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(isOwnProfile && isInEditMode){
			editSaveMenuItem=menu.add(0, R.id.save, 0, R.string.save_changes);
			editSaveMenuItem.setIcon(R.drawable.ic_save_24px);
			editSaveMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			editSaveMenuItem.setVisible(!isActionButtonInView());
			return;
		}
		if(relationship==null && !isOwnProfile)
			return;
		inflater.inflate(isOwnProfile ? R.menu.profile_own : R.menu.profile, menu);
		menu.findItem(R.id.share).setTitle(getString(R.string.share_user, account.getDisplayUsername()));
		if(isOwnProfile)
			return;

		menu.findItem(R.id.mute).setTitle(getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getDisplayUsername()));
		menu.findItem(R.id.block).setTitle(getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getDisplayUsername()));
		menu.findItem(R.id.report).setTitle(getString(R.string.report_user, account.getDisplayUsername()));
		if(relationship.following)
			menu.findItem(R.id.hide_boosts).setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user, account.getDisplayUsername()));
		else
			menu.findItem(R.id.hide_boosts).setVisible(false);
		if(!account.isLocal())
			menu.findItem(R.id.block_domain).setTitle(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
		else
			menu.findItem(R.id.block_domain).setVisible(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.share){
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, account.url);
			startActivity(Intent.createChooser(intent, item.getTitle()));
		}else if(id==R.id.mute){
			confirmToggleMuted();
		}else if(id==R.id.block){
			confirmToggleBlocked();
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account.getDomain(), relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship();
			});
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs)
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
		}else if(id==R.id.bookmarks){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), BookmarkedStatusListFragment.class, args);
		}else if(id==R.id.favorites){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FavoritedStatusListFragment.class, args);
		}else if(id==R.id.save){
			if(isInEditMode)
				saveAndExitEditMode();
		}
		return true;
	}

	private void loadRelationship(){
		new GetAccountRelationships(Collections.singletonList(account.id))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						if(!result.isEmpty()){
							relationship=result.get(0);
							updateRelationship();
						}
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	private void updateRelationship(){
		invalidateOptionsMenu();
		actionButton.setVisibility(View.VISIBLE);
		UiUtils.setRelationshipToActionButtonM3(relationship, actionButton);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		followsYouView.setVisibility(relationship.followedBy ? View.VISIBLE : View.GONE);
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
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
		if(newTabBarIsAtTop!=tabBarIsAtTop){
			tabBarIsAtTop=newTabBarIsAtTop;

			if(tabBarIsAtTop){
				// ScrollView would sometimes leave 1 pixel unscrolled, force it into the correct scrollY
				int maxY=scrollView.getChildAt(0).getHeight()-scrollView.getHeight();
				if(scrollView.getScrollY()!=maxY)
					scrollView.scrollTo(0, maxY);
			}

			if(tabBarColorAnim!=null)
				tabBarColorAnim.cancel();
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
		if(isInEditMode && editSaveMenuItem!=null){
			boolean buttonInView=isActionButtonInView();
			if(buttonInView==editSaveMenuItem.isVisible()){
				editSaveMenuItem.setVisible(!buttonInView);
			}
		}
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> featuredFragment;
			case 1 -> timelineFragment;
			case 2 -> aboutFragment;
			default -> throw new IllegalStateException();
		};
	}

	private RecyclerView getScrollableRecyclerView(){
		return getFragmentForPage(pager.getCurrentItem()).getView().findViewById(R.id.list);
	}

	private void onActionButtonClick(View v){
		if(isOwnProfile){
			if(!isInEditMode)
				loadAccountInfoAndEnterEditMode();
			else
				saveAndExitEditMode();
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

	private void loadAccountInfoAndEnterEditMode(){
		if(editModeLoading)
			return;
		editModeLoading=true;
		setActionProgressVisible(true);
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						enterEditMode(result);
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void enterEditMode(Account account){
		if(isInEditMode)
			throw new IllegalStateException();
		isInEditMode=true;
		invalidateOptionsMenu();
		pager.setUserInputEnabled(false);
		actionButton.setText(R.string.save_changes);
		pager.setCurrentItem(2);
		for(int i=0;i<3;i++){
			tabbar.getTabAt(i).view.setEnabled(false);
		}
		Drawable overlay=getResources().getDrawable(R.drawable.edit_avatar_overlay).mutate();
		avatar.setForeground(overlay);

		Toolbar toolbar=getToolbar();
		Drawable close=getToolbarContext().getDrawable(R.drawable.ic_baseline_close_24).mutate();
		close.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
		toolbar.setNavigationIcon(close);
		toolbar.setNavigationContentDescription(R.string.discard);

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);

		name.setVisibility(View.GONE);
		username.setVisibility(View.GONE);
		bio.setVisibility(View.GONE);
		countersLayout.setVisibility(View.GONE);

		nameEditWrap.setVisibility(View.VISIBLE);
		nameEdit.setText(account.displayName);

		bioEditWrap.setVisibility(View.VISIBLE);
		bioEdit.setText(account.source.note);

		aboutFragment.enterEditMode(account.source.fields);
	}

	private void exitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		isInEditMode=false;

		invalidateOptionsMenu();
		actionButton.setText(R.string.edit_profile);
		for(int i=0;i<3;i++){
			tabbar.getTabAt(i).view.setEnabled(true);
		}
		pager.setUserInputEnabled(true);
		avatar.setForeground(null);

		Toolbar toolbar=getToolbar();
		if(canGoBack()){
			Drawable back=getToolbarContext().getDrawable(R.drawable.ic_arrow_back).mutate();
			back.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
			toolbar.setNavigationIcon(back);
			toolbar.setNavigationContentDescription(0);
		}else{
			toolbar.setNavigationIcon(null);
		}
		editSaveMenuItem=null;

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);
		nameEditWrap.setVisibility(View.GONE);
		bioEditWrap.setVisibility(View.GONE);
		name.setVisibility(View.VISIBLE);
		username.setVisibility(View.VISIBLE);
		bio.setVisibility(View.VISIBLE);
		countersLayout.setVisibility(View.VISIBLE);

		bindHeaderView();
	}

	private void saveAndExitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		setActionProgressVisible(true);
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), editNewAvatar, editNewCover, aboutFragment.getFields())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						account=result;
						AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						exitEditMode();
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
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

	@Override
	public boolean onBackPressed(){
		if(isInEditMode){
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.discard_changes)
					.setPositiveButton(R.string.discard, (dlg, btn)->exitEditMode())
					.setNegativeButton(R.string.cancel, null)
					.show();
			return true;
		}
		return false;
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
		if(isInEditMode){
			startImagePicker(AVATAR_RESULT);
		}else{
			Drawable ava=avatar.getDrawable();
			if(ava==null)
				return;
			int radius=V.dp(25);
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(account.avatar, ava), 0,
					new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->currentPhotoViewer=null, ()->ava, null, null));
		}
	}

	private void onCoverClick(View v){
		if(isInEditMode){
			startImagePicker(COVER_RESULT);
		}else{
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable)
				return;
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(account.header, drawable), 0,
					new SingleImagePhotoViewerListener(cover, cover, null, this, ()->currentPhotoViewer=null, ()->drawable, ()->avatarBorder.setTranslationZ(2), ()->avatarBorder.setTranslationZ(0)));
		}
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(!AccountSessionManager.getInstance().isSelf(accountID, account)){
			args.putString("prefilledText", '@'+account.acct+' ');
		}
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void startImagePicker(int requestCode){
		Intent intent=UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				editNewAvatar=data.getData();
				ViewImageLoader.loadWithoutAnimation(avatar, null, new UrlImageLoaderRequest(editNewAvatar, V.dp(100), V.dp(100)));
			}else if(requestCode==COVER_RESULT){
				editNewCover=data.getData();
				ViewImageLoader.loadWithoutAnimation(cover, null, new UrlImageLoaderRequest(editNewCover, V.dp(1000), V.dp(1000)));
			}
		}
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

	private class ProfilePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=tabViews[viewType];
			((ViewGroup)view.getParent()).removeView(view);
			view.setVisibility(View.VISIBLE);
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(holder.itemView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
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
			return loaded ? 3 : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
