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
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
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
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.drawables.CoverOverlayGradientDrawable;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProgressBarButton;
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

public class ProfileFragment extends LoaderFragment implements OnBackPressedListener, ScrollableToTop{
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder;
	private TextView name, username, bio, followersCount, followersLabel, followingCount, followingLabel, postsCount, postsLabel;
	private ProgressBarButton actionButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private AccountTimelineFragment postsFragment, postsWithRepliesFragment, mediaFragment;
	private ProfileAboutFragment aboutFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private CoverOverlayGradientDrawable coverGradient=new CoverOverlayGradientDrawable();
	private float titleTransY;
	private View postsBtn, followersBtn, followingBtn;
	private EditText nameEdit, bioEdit;
	private ProgressBar actionProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private TextView followsYouView;

	private Account account;
	private String accountID;
	private Relationship relationship;
	private int statusBarHeight;
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

	public ProfileFragment(){
		super(R.layout.loader_fragment_overlay_toolbar);
	}

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
		postsCount=content.findViewById(R.id.posts_count);
		postsLabel=content.findViewById(R.id.posts_label);
		postsBtn=content.findViewById(R.id.posts_btn);
		actionButton=content.findViewById(R.id.profile_action_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		nameEdit=content.findViewById(R.id.name_edit);
		bioEdit=content.findViewById(R.id.bio_edit);
		actionProgress=content.findViewById(R.id.action_progress);
		fab=content.findViewById(R.id.fab);
		followsYouView=content.findViewById(R.id.follows_you);

		avatar.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(25));
			}
		});
		avatar.setClipToOutline(true);

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				Toolbar toolbar=getToolbar();
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-toolbar.getLayoutParams().height-statusBarHeight-V.dp(38);
				coverGradient.setTopPadding(statusBarHeight+toolbar.getLayoutParams().height);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[4];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.profile_posts;
				case 1 -> R.id.profile_posts_with_replies;
				case 2 -> R.id.profile_media;
				case 3 -> R.id.profile_about;
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

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		tabbar.setTabTextSize(V.dp(16));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.posts;
					case 1 -> R.string.posts_and_replies;
					case 2 -> R.string.media;
					case 3 -> R.string.profile_about;
					default -> throw new IllegalStateException();
				});
			}
		});

		cover.setForeground(coverGradient);
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
							if(postsFragment.loaded)
								postsFragment.onRefresh();
							if(postsWithRepliesFragment.loaded)
								postsWithRepliesFragment.onRefresh();
							if(mediaFragment.loaded)
								mediaFragment.onRefresh();
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
		if(postsFragment==null){
			postsFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.DEFAULT, true);
			postsWithRepliesFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.INCLUDE_REPLIES, false);
			mediaFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.MEDIA, false);
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

		scrollView.setOnScrollChangeListener(this::onScrollChanged);
		titleTransY=getToolbar().getLayoutParams().height;
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		statusBarHeight=insets.getSystemWindowInsetTop();
		if(contentView!=null){
			((ViewGroup.MarginLayoutParams) getToolbar().getLayoutParams()).topMargin=statusBarHeight;
			refreshLayout.setProgressViewEndTarget(true, statusBarHeight+refreshLayout.getProgressCircleDiameter()+V.dp(24));
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24)+insetBottom;
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}else{
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24);
			}
		}
		super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
	}

	private void applyChildWindowInsets(){
		if(postsFragment!=null && postsFragment.isAdded() && childInsets!=null){
			postsFragment.onApplyWindowInsets(childInsets);
			postsWithRepliesFragment.onApplyWindowInsets(childInsets);
			mediaFragment.onApplyWindowInsets(childInsets);
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
		postsCount.setText(UiUtils.abbreviateNumber(account.statusesCount));
		followersLabel.setText(getResources().getQuantityString(R.plurals.followers, (int)Math.min(999, account.followersCount)));
		followingLabel.setText(getResources().getQuantityString(R.plurals.following, (int)Math.min(999, account.followingCount)));
		postsLabel.setText(getResources().getQuantityString(R.plurals.posts, (int)Math.min(999, account.statusesCount)));

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		if(AccountSessionManager.getInstance().isSelf(accountID, account)){
			actionButton.setText(R.string.edit_profile);
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
		getToolbar().setBackgroundColor(0);
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(isOwnProfile && isInEditMode){
			Button cancelButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_Button_Secondary_LightOnDark);
			cancelButton.setText(R.string.cancel);
			cancelButton.setOnClickListener(v->exitEditMode());
			FrameLayout wrap=new FrameLayout(getActivity());
			wrap.addView(cancelButton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));
			wrap.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(8));
			wrap.setClipToPadding(false);
			MenuItem item=menu.add(R.string.cancel);
			item.setActionView(wrap);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
		}
		return true;
	}

	@Override
	protected int getToolbarResource(){
		return R.layout.profile_toolbar;
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
		UiUtils.setRelationshipToActionButton(relationship, actionButton);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		followsYouView.setVisibility(relationship.followedBy ? View.VISIBLE : View.GONE);
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		int topBarsH=getToolbar().getHeight()+statusBarHeight;
		if(scrollY>avatarBorder.getTop()-topBarsH){
			float avaAlpha=Math.max(1f-((scrollY-(avatarBorder.getTop()-topBarsH))/(float)V.dp(38)), 0f);
			avatarBorder.setAlpha(avaAlpha);
		}else{
			avatarBorder.setAlpha(1f);
		}
		if(scrollY>cover.getHeight()-topBarsH){
			cover.setTranslationY(scrollY-(cover.getHeight()-topBarsH));
			cover.setTranslationZ(V.dp(10));
			cover.setTransform(cover.getHeight()/2f-topBarsH/2f, 1f);
		}else{
			cover.setTranslationY(0f);
			cover.setTranslationZ(0f);
			cover.setTransform(scrollY/2f, 1f);
		}
		coverGradient.setTopOffset(scrollY);
		cover.invalidate();
		titleTransY=getToolbar().getHeight();
		if(scrollY>name.getTop()-topBarsH){
			titleTransY=Math.max(0f, titleTransY-(scrollY-(name.getTop()-topBarsH)));
		}
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		if(currentPhotoViewer!=null){
			currentPhotoViewer.offsetView(0, oldScrollY-scrollY);
		}
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> postsFragment;
			case 1 -> postsWithRepliesFragment;
			case 2 -> mediaFragment;
			case 3 -> aboutFragment;
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
		actionButton.setText(R.string.done);
		pager.setCurrentItem(3);
		ArrayList<Animator> animators=new ArrayList<>();
		for(int i=0;i<3;i++){
			animators.add(ObjectAnimator.ofFloat(tabbar.getTabAt(i).view, View.ALPHA, .3f));
			tabbar.getTabAt(i).view.setEnabled(false);
		}
		Drawable overlay=getResources().getDrawable(R.drawable.edit_avatar_overlay).mutate();
		avatar.setForeground(overlay);
		animators.add(ObjectAnimator.ofInt(overlay, "alpha", 0, 255));

		nameEdit.setVisibility(View.VISIBLE);
		nameEdit.setText(account.displayName);
		RelativeLayout.LayoutParams lp=(RelativeLayout.LayoutParams) username.getLayoutParams();
		lp.addRule(RelativeLayout.BELOW, R.id.name_edit);
		username.getParent().requestLayout();
		animators.add(ObjectAnimator.ofFloat(nameEdit, View.ALPHA, 0f, 1f));

		bioEdit.setVisibility(View.VISIBLE);
		bioEdit.setText(account.source.note);
		animators.add(ObjectAnimator.ofFloat(bioEdit, View.ALPHA, 0f, 1f));
		animators.add(ObjectAnimator.ofFloat(bio, View.ALPHA, 0f));

		animators.add(ObjectAnimator.ofFloat(postsBtn, View.ALPHA, .3f));
		animators.add(ObjectAnimator.ofFloat(followersBtn, View.ALPHA, .3f));
		animators.add(ObjectAnimator.ofFloat(followingBtn, View.ALPHA, .3f));

		AnimatorSet set=new AnimatorSet();
		set.playTogether(animators);
		set.setDuration(300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.start();

		aboutFragment.enterEditMode(account.source.fields);
	}

	private void exitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		isInEditMode=false;

		invalidateOptionsMenu();
		ArrayList<Animator> animators=new ArrayList<>();
		actionButton.setText(R.string.edit_profile);
		for(int i=0;i<3;i++){
			animators.add(ObjectAnimator.ofFloat(tabbar.getTabAt(i).view, View.ALPHA, 1f));
		}
		animators.add(ObjectAnimator.ofInt(avatar.getForeground(), "alpha", 0));
		animators.add(ObjectAnimator.ofFloat(nameEdit, View.ALPHA, 0f));
		animators.add(ObjectAnimator.ofFloat(bioEdit, View.ALPHA, 0f));
		animators.add(ObjectAnimator.ofFloat(bio, View.ALPHA, 1f));
		animators.add(ObjectAnimator.ofFloat(postsBtn, View.ALPHA, 1f));
		animators.add(ObjectAnimator.ofFloat(followersBtn, View.ALPHA, 1f));
		animators.add(ObjectAnimator.ofFloat(followingBtn, View.ALPHA, 1f));

		AnimatorSet set=new AnimatorSet();
		set.playTogether(animators);
		set.setDuration(200);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				for(int i=0;i<3;i++){
					tabbar.getTabAt(i).view.setEnabled(true);
				}
				pager.setUserInputEnabled(true);
				nameEdit.setVisibility(View.GONE);
				bioEdit.setVisibility(View.GONE);
				RelativeLayout.LayoutParams lp=(RelativeLayout.LayoutParams) username.getLayoutParams();
				lp.addRule(RelativeLayout.BELOW, R.id.name);
				username.getParent().requestLayout();
				avatar.setForeground(null);
			}
		});
		set.start();

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
			exitEditMode();
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
		Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				editNewAvatar=data.getData();
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(editNewAvatar, V.dp(100), V.dp(100)));
			}else if(requestCode==COVER_RESULT){
				editNewCover=data.getData();
				ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(editNewCover, V.dp(1000), V.dp(1000)));
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
			return loaded ? 4 : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
