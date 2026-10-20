package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.assist.AssistContent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.PushNotificationReceiver;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV1;
import org.joinmastodon.android.api.requests.notifications.GetUnreadNotificationsCount;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.events.SelfAccountUpdatedEvent;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.fragments.discover.DiscoverFragment;
import org.joinmastodon.android.fragments.onboarding.OnboardingFollowSuggestionsFragment;
import org.joinmastodon.android.fragments.profile.ProfileMenuFragment;
import org.joinmastodon.android.fragments.settings.SettingsServerFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.SectionHeaderListItem;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredUnderlinedLinkSpan;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.ui.views.TabBar;
import org.joinmastodon.android.utils.ObjectIdComparator;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CustomViewHelper;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;

public class HomeFragment extends AppKitFragment implements AssistContentProviderFragment{
	private FragmentRootLinearLayout content;
	private HomeTimelineFragment homeTimelineFragment;
	private MessagesFragment messagesFragment;
	private NotificationsListFragment notificationsFragment;
	private DiscoverFragment searchFragment;
	private ProfileMenuFragment profileMenuFragment;
	private TabBar tabBar;
	private View tabBarWrap;
	private ImageView tabBarAvatar;
	@IdRes
	private int currentTab=R.id.tab_home;
	private TextView notificationsBadge;

	private UsableRecyclerView menuList;
	private ArrayList<ListItem<FollowList>> listsMenuItems=new ArrayList<>();
	private ArrayList<ListItem<Hashtag>> hashtagsMenuItems=new ArrayList<>();
	private ArrayList<ListItem<Void>> followedHashtagsTitleMenuItems=new ArrayList<>();
	private GenericListItemsAdapter<FollowList> listsMenuAdapter=new GenericListItemsAdapter<>(listsMenuItems);
	private GenericListItemsAdapter<Hashtag> followedTagsMenuAdapter=new GenericListItemsAdapter<>(hashtagsMenuItems);
	private GenericListItemsAdapter<Void> followedHashtagsMenuTitleAdapter=new GenericListItemsAdapter<>(followedHashtagsTitleMenuItems);
	private SectionHeaderListItem followedHashtagsTitleMenuItem;
	private ListItem<Void> homeTimelineMenuItem, localTimelineMenuItem;
	private ListItem<?> selectedMenuItem;

	private String accountID;
	private DrawerLayout drawerLayout;
	private DrawerMenuWrapperView menuView;
	private boolean closeDrawerOnHide;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.app_name);

		setRetainInstance(true);

		if(savedInstanceState==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			homeTimelineFragment=new HomeTimelineFragment();
			homeTimelineFragment.setArguments(args);
			args=new Bundle(args);
			args.putBoolean("noAutoLoad", true);
			searchFragment=new DiscoverFragment();
			searchFragment.setArguments(args);
			messagesFragment=new MessagesFragment();
			messagesFragment.setArguments(args);
			notificationsFragment=new NotificationsListFragment();
			notificationsFragment.setArguments(args);
			args=new Bundle(args);
			profileMenuFragment=new ProfileMenuFragment();
			profileMenuFragment.setArguments(args);
		}

		E.register(this);
		updateListsAndHashtags(false);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		drawerLayout=new DrawerLayout(getActivity());

		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		drawerLayout.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		FrameLayout fragmentContainer=new FrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.tab_bar, content);
		tabBar=content.findViewById(R.id.tabbar);
		tabBar.setListeners(this::onTabSelected, this::onTabLongClick);
		tabBarWrap=content.findViewById(R.id.tabbar_wrap);

		tabBarAvatar=tabBar.findViewById(R.id.tab_profile_ava);
		tabBarAvatar.setOutlineProvider(OutlineProviders.OVAL);
		tabBarAvatar.setClipToOutline(true);
		Account self=AccountSessionManager.getInstance().getAccount(accountID).self;
		ViewImageLoader.loadWithoutAnimation(tabBarAvatar, null, new UrlImageLoaderRequest(self.avatar, V.dp(24), V.dp(24)));

		notificationsBadge=tabBar.findViewById(R.id.notifications_badge);
		notificationsBadge.setVisibility(View.GONE);

		if(savedInstanceState==null){
			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, homeTimelineFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, searchFragment).hide(searchFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, messagesFragment).hide(messagesFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, profileMenuFragment).hide(profileMenuFragment)
					.commit();

			String defaultTab=getArguments().getString("tab");
			if("notifications".equals(defaultTab)){
				fragmentContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						fragmentContainer.getViewTreeObserver().removeOnPreDrawListener(this);
						tabBar.selectTab(R.id.tab_notifications);
						onTabSelected(R.id.tab_notifications);
						return true;
					}
				});
			}
		}
		tabBar.selectTab(currentTab);

		menuView=new DrawerMenuWrapperView(getActivity());
		menuView.setOrientation(LinearLayout.VERTICAL);
		menuView.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnPrimary));

		menuList=new UsableRecyclerView(getActivity());
		menuView.addView(menuList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		menuList.setLayoutManager(new LinearLayoutManager(getActivity()));

		MergeRecyclerAdapter mergeAdapter=new SelectableMergeRecyclerAdapter();
		AccountSession session=AccountSessionManager.get(accountID);
		List<ListItem<Void>> topItems=new ArrayList<>();
		homeTimelineMenuItem=new ListItem<>(R.string.tab_home, 0, R.drawable.ic_phosphor_house_fill, item->{
			drawerLayout.close();
			homeTimelineFragment.setListMode(HomeTimelineFragment.ListMode.FOLLOWING, null);
			selectMenuItem(item);
		});
		selectedMenuItem=homeTimelineMenuItem;
		topItems.add(homeTimelineMenuItem);
		if(session.canAccessLocalTimeline()){
			localTimelineMenuItem=new ListItem<>(R.string.local_timeline, 0, R.drawable.ic_phosphor_fediverse, item->{
				drawerLayout.close();
				homeTimelineFragment.setListMode(HomeTimelineFragment.ListMode.LOCAL, null);
				selectMenuItem(item);
			});
			topItems.add(localTimelineMenuItem);
		}
		topItems.get(topItems.size()-1).dividerAfter=true;
		topItems.add(new SectionHeaderListItem(R.string.lists));
		topItems.add(new ListItem<>(R.string.create_list, 0, R.drawable.ic_phosphor_plus_light, item->closeDrawerAndNavigate(CreateListFragment.class)));
		topItems.add(new ListItem<>(R.string.manage_lists, 0, R.drawable.ic_phosphor_plus_light, item->{ // TODO where do we put this item?
			closeDrawerAndNavigate(ManageListsFragment.class);
		}));
		mergeAdapter.addAdapter(new GenericListItemsAdapter<>(topItems));
		mergeAdapter.addAdapter(listsMenuAdapter);
		mergeAdapter.addAdapter(followedHashtagsMenuTitleAdapter);
		mergeAdapter.addAdapter(followedTagsMenuAdapter);
		followedHashtagsTitleMenuItem=new SectionHeaderListItem(R.string.followed_hashtags);

		menuList.setAdapter(mergeAdapter);
		menuList.setPadding(0, V.dp(12), 0, V.dp(12));
		menuList.setClipToPadding(false);
		menuList.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3OutlineVariant, 1, 28, 28, vh->vh instanceof ListItemViewHolder<?> ivh && ivh.getItem().dividerAfter));
		menuList.setItemAnimator(new BetterItemAnimator());

		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		menuView.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1)));

		LinearLayout menuFooter=new LinearLayout(getActivity());
		menuFooter.setOrientation(LinearLayout.HORIZONTAL);

		TextView footerServerDomain=new TextView(getActivity());
		footerServerDomain.setTextAppearance(R.style.m3_body_small);
		footerServerDomain.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
		footerServerDomain.setText(session.getInstanceInfo().getDomain());
		footerServerDomain.setSingleLine();
		footerServerDomain.setPaddingRelative(V.dp(16), 0, V.dp(8), 0);
		footerServerDomain.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		menuFooter.addView(footerServerDomain, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

		LinkedTextView footerLinks=new LinkedTextView(getActivity());
		footerLinks.setTextAppearance(R.style.m3_body_small);
		footerLinks.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
		footerLinks.setPaddingRelative(V.dp(8), 0, V.dp(16), 0);
		SpannableStringBuilder footerLinksText=new SpannableStringBuilder();
		footerLinksText.append(getString(R.string.about_server),
				new NonColoredUnderlinedLinkSpan("", span->closeDrawerAndNavigate(SettingsServerFragment.class), LinkSpan.Type.CUSTOM, null, null, null), 0);
		footerLinksText.append(" ", new SpacerSpan(V.dp(8), 1), 0);
		footerLinksText.append(getString(R.string.privacy_policy_short),
				new NonColoredUnderlinedLinkSpan("", span->UiUtils.launchWebBrowser(getActivity(), session.getInstanceInfo().getPrivacyPolicyURL()), LinkSpan.Type.CUSTOM, null, null, null), 0);
		footerLinksText.append(" ", new SpacerSpan(V.dp(8), 1), 0);
		footerLinksText.append(getString(R.string.terms_of_service_short),
				new NonColoredUnderlinedLinkSpan("", span->UiUtils.launchWebBrowser(getActivity(), session.getInstanceInfo().getTermsOfServiceURL()), LinkSpan.Type.CUSTOM, null, null, null), 0);
		footerLinks.setText(footerLinksText);
		footerLinks.setSingleLine();
		footerLinks.setGravity(Gravity.CENTER_VERTICAL);
		menuFooter.addView(footerLinks, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

		menuView.addView(menuFooter, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(64)));

		drawerLayout.addView(menuView, new DrawerLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
		drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener(){
			@Override
			public void onDrawerOpened(@NonNull View drawerView){
				updateListsAndHashtags(true);
			}
		});

		return drawerLayout;
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		if(savedInstanceState==null || homeTimelineFragment!=null)
			return;
		homeTimelineFragment=(HomeTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "homeTimelineFragment");
		searchFragment=(DiscoverFragment) getChildFragmentManager().getFragment(savedInstanceState, "searchFragment");
		messagesFragment=(MessagesFragment) getChildFragmentManager().getFragment(savedInstanceState, "messagesFragment");
		notificationsFragment=(NotificationsListFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		profileMenuFragment=(ProfileMenuFragment) getChildFragmentManager().getFragment(savedInstanceState, "profileFragment");
		currentTab=savedInstanceState.getInt("selectedTab");
		tabBar.selectTab(currentTab);
		Fragment current=fragmentForTab(currentTab);
		getChildFragmentManager().beginTransaction()
				.hide(homeTimelineFragment)
				.hide(searchFragment)
				.hide(messagesFragment)
				.hide(notificationsFragment)
				.hide(profileMenuFragment)
				.show(current)
				.commit();
		maybeTriggerLoading(current);
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		fragmentForTab(currentTab).onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		int bottomInset;
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			bottomInset=inset>0 ? Math.max(inset, V.dp(24)) : 0;
			tabBarWrap.setPadding(0, 0, 0, bottomInset);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), bottomInset=insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnlyInsets=insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);
		homeTimelineFragment.onApplyWindowInsets(topOnlyInsets);
		searchFragment.onApplyWindowInsets(topOnlyInsets);
		messagesFragment.onApplyWindowInsets(topOnlyInsets);
		notificationsFragment.onApplyWindowInsets(topOnlyInsets);
		profileMenuFragment.onApplyWindowInsets(topOnlyInsets);

		menuList.setPadding(0, V.dp(12), 0, V.dp(12));
		menuView.setPadding(0, insets.getSystemWindowInsetTop(), 0, bottomInset);
	}

	private Fragment fragmentForTab(@IdRes int tab){
		if(tab==R.id.tab_home){
			return homeTimelineFragment;
		}else if(tab==R.id.tab_search){
			return searchFragment;
		}else if(tab==R.id.tab_messages){
			return messagesFragment;
		}else if(tab==R.id.tab_notifications){
			return notificationsFragment;
		}else if(tab==R.id.tab_profile){
			return profileMenuFragment;
		}
		throw new IllegalArgumentException();
	}

	public void setCurrentTab(@IdRes int tab){
		if(tab==currentTab)
			return;
		tabBar.selectTab(tab);
		onTabSelected(tab);
	}

	private void onTabSelected(@IdRes int tab){
		Fragment newFragment=fragmentForTab(tab);
		if(tab==currentTab){
			if(newFragment instanceof ScrollableToTop scrollable)
				scrollable.scrollToTop();
			return;
		}
		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(newFragment).commit();
		maybeTriggerLoading(newFragment);
		currentTab=tab;
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
		drawerLayout.setDrawerLockMode(tab==R.id.tab_home ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	private void maybeTriggerLoading(Fragment newFragment){
		if(newFragment instanceof LoaderFragment lf){
			if(!lf.loaded && !lf.dataLoading)
				lf.loadData();
		}else if(newFragment instanceof DiscoverFragment){
			((DiscoverFragment) newFragment).loadData();
		}
		if(newFragment instanceof NotificationsListFragment){
			NotificationManager nm=getActivity().getSystemService(NotificationManager.class);
			nm.cancel(accountID, PushNotificationReceiver.NOTIFICATION_ID);
		}
	}

	private boolean onTabLongClick(@IdRes int tab){
		if(tab==R.id.tab_profile){
			ArrayList<String> options=new ArrayList<>();
			for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
				options.add(session.self.displayName+"\n("+session.self.username+"@"+session.getUsernameDomain()+")");
			}
			new AccountSwitcherSheet(getActivity(), this).show();
			return true;
		}
		if(tab==R.id.tab_home && BuildConfig.DEBUG){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), OnboardingFollowSuggestionsFragment.class, args);
		}
		return false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("selectedTab", currentTab);
		getChildFragmentManager().putFragment(outState, "homeTimelineFragment", homeTimelineFragment);
		getChildFragmentManager().putFragment(outState, "searchFragment", searchFragment);
		getChildFragmentManager().putFragment(outState, "messagesFragment", messagesFragment);
		getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
		getChildFragmentManager().putFragment(outState, "profileFragment", profileMenuFragment);
	}

	@Override
	protected void onShown(){
		super.onShown();
		reloadNotificationsForUnreadCount();
	}

	private void reloadNotificationsForUnreadCount(){
		Instance instance=AccountSessionManager.get(accountID).getInstanceInfo();
		if(instance==null)
			return;
		if(instance.getApiVersion()>=2){
			EnumSet<NotificationType> excludeTypes=EnumSet.noneOf(NotificationType.class);
			AccountLocalPreferences lp=AccountSessionManager.get(accountID).getLocalPreferences();
			if(!lp.adminReportsNotifications)
				excludeTypes.add(NotificationType.ADMIN_REPORT);
			if(!lp.adminSignupsNotifications)
				excludeTypes.add(NotificationType.ADMIN_SIGNUP);
			new GetUnreadNotificationsCount(null, NotificationType.getGroupableTypes(), excludeTypes)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(GetUnreadNotificationsCount.Response result){
							updateUnreadNotificationsBadge(result.count, false);
						}

						@Override
						public void onError(ErrorResponse error){

						}
					})
					.exec(accountID);
		}else{
			List<Notification>[] notifications=new List[]{null};
			String[] marker={null};
			AccountSessionManager.get(accountID).reloadNotificationsMarker(m->{
				marker[0]=m;
				if(notifications[0]!=null){
					updateUnreadCountV1(notifications[0], marker[0]);
				}
			});

			new GetNotificationsV1(null, 40, EnumSet.allOf(NotificationType.class))
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(List<Notification> result){
							notifications[0]=result;
							if(marker[0]!=null)
								updateUnreadCountV1(notifications[0], marker[0]);
						}

						@Override
						public void onError(ErrorResponse error){}
					}).exec(accountID);
		}
	}

	@SuppressLint("DefaultLocale")
	private void updateUnreadCountV1(List<Notification> notifications, String marker){
		if(notifications.isEmpty() || ObjectIdComparator.INSTANCE.compare(notifications.get(0).id, marker)<=0){
			updateUnreadNotificationsBadge(0, false);
		}else{
			if(ObjectIdComparator.INSTANCE.compare(notifications.get(notifications.size()-1).id, marker)>0){
				updateUnreadNotificationsBadge(notifications.size(), true);
			}else{
				int count=0;
				for(Notification n:notifications){
					if(n.id.equals(marker))
						break;
					count++;
				}
				updateUnreadNotificationsBadge(count, false);
			}
		}
	}

	private void updateUnreadNotificationsBadge(int count, boolean more){
		if(count==0){
			notificationsBadge.setVisibility(View.GONE);
		}else{
			notificationsBadge.setVisibility(View.VISIBLE);
			notificationsBadge.setText(String.format(more ? "%d+" : "%d", count));
		}
	}

	@Subscribe
	public void onNotificationsMarkerUpdated(NotificationsMarkerUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(ev.clearUnread)
			notificationsBadge.setVisibility(View.GONE);
	}

	@Subscribe
	public void onStatusDisplaySettingsChanged(StatusDisplaySettingsChangedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(homeTimelineFragment.loaded)
			homeTimelineFragment.rebuildAllDisplayItems();
		if(notificationsFragment.loaded)
			notificationsFragment.rebuildAllDisplayItems();
	}

	@Subscribe
	public void onAccountUpdated(SelfAccountUpdatedEvent ev){
		if(ev.accountID().equals(accountID)){
			ViewImageLoader.loadWithoutAnimation(tabBarAvatar, null, new UrlImageLoaderRequest(ev.account().avatar, V.dp(24), V.dp(24)));
		}
	}

	@Override
	public void onProvideAssistContent(AssistContent content){
		if(fragmentForTab(currentTab) instanceof AssistContentProviderFragment provider){
			provider.onProvideAssistContent(content);
		}
	}

	public void openDrawer(){
		drawerLayout.open();
	}

	private void updateListsAndHashtags(boolean alsoReload){
		AccountSessionManager.get(accountID).getCacheController().getLists(new Callback<>(){
			@Override
			public void onSuccess(List<FollowList> result){
				ArrayList<ListItem<FollowList>> oldItems=new ArrayList<>(listsMenuItems);
				listsMenuItems.clear();
				for(FollowList list:result){
					listsMenuItems.add(new ListItem<>(list.title, null, R.drawable.ic_phosphor_rss_simple_light, HomeFragment.this::openList, list));
				}
				listsMenuItems.sort(Comparator.comparing(i->i.title, Collator.getInstance()));
				if(!listsMenuItems.isEmpty())
					listsMenuItems.get(listsMenuItems.size()-1).dividerAfter=true;
				if(oldItems.contains(selectedMenuItem)){
					FollowList currentList=homeTimelineFragment.getCurrentList();
					if(currentList!=null){
						String currentListID=currentList.id;
						for(ListItem<FollowList> item:listsMenuItems){
							if(item.parentObject.id.equals(currentListID)){
								selectedMenuItem=item;
								break;
							}
						}
					}
				}
				UiUtils.updateList(oldItems, listsMenuItems, menuList, listsMenuAdapter, (a, b)->a.parentObject.id.equals(b.parentObject.id), false);
			}

			@Override
			public void onError(ErrorResponse error){}
		}, alsoReload);

		AccountSessionManager.get(accountID).getCacheController().getFollowedHashtags(new Callback<>(){
			@Override
			public void onSuccess(List<Hashtag> result){
				ArrayList<ListItem<Hashtag>> oldItems=new ArrayList<>(hashtagsMenuItems);
				hashtagsMenuItems.clear();
				for(Hashtag tag:result){
					hashtagsMenuItems.add(new ListItem<>('#'+tag.name, null, 0, HomeFragment.this::openHashtag, tag));
				}
				UiUtils.updateList(oldItems, hashtagsMenuItems, menuList, followedTagsMenuAdapter, (a, b)->a.parentObject.id.equals(b.parentObject.id), false);
				boolean wasEmpty=oldItems.isEmpty();
				boolean isEmpty=hashtagsMenuItems.isEmpty();
				if(wasEmpty!=isEmpty){
					if(isEmpty){
						followedHashtagsTitleMenuItems.clear();
						followedHashtagsMenuTitleAdapter.notifyItemRemoved(0);
					}else{
						followedHashtagsTitleMenuItems.add(followedHashtagsTitleMenuItem);
						followedHashtagsMenuTitleAdapter.notifyItemInserted(0);
					}
				}
			}

			@Override
			public void onError(ErrorResponse error){}
		}, alsoReload);
	}

	private void openList(ListItem<FollowList> item){
		drawerLayout.close();
		homeTimelineFragment.setListMode(HomeTimelineFragment.ListMode.LIST, item.parentObject);
		selectMenuItem(item);
	}

	private void openHashtag(ListItem<Hashtag> item){
		closeDrawerOnHide=true;
		UiUtils.openHashtagTimeline(getActivity(), accountID, item.parentObject);
	}

	private void closeDrawerAndNavigate(Class<? extends Fragment> to){
		closeDrawerOnHide=true;
		Nav.go(getActivity(), to, UiUtils.makeAccountArgs(accountID));
	}

	private void selectMenuItem(ListItem<?> item){
		if(selectedMenuItem!=item){
			for(int i=0;i<menuList.getChildCount();i++){
				if(menuList.getChildViewHolder(menuList.getChildAt(i)) instanceof SimpleListItemViewHolder ih){
					ListItem<?> holderItem=ih.getItem();
					if(holderItem==selectedMenuItem)
						ih.itemView.setSelected(false);
					else if(holderItem==item)
						ih.itemView.setSelected(true);
				}
			}
			selectedMenuItem=item;
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		// This is needed because if the drawer is closed immediately, it's visually jarring.
		// If it's closed with an animation, this also breaks the back callback because it removes its callback
		// only after the animation finishes. Which it doesn't because the fragment gets hidden before the animation would finish.
		if(closeDrawerOnHide){
			drawerLayout.closeDrawer(Gravity.START, false);
			closeDrawerOnHide=false;
		}
	}

	private static class DrawerMenuWrapperView extends LinearLayout implements CustomViewHelper{
		public DrawerMenuWrapperView(Context context){
			super(context);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			int parentW=((ViewGroup)getParent()).getMeasuredWidth();
			super.onMeasure(MeasureSpec.EXACTLY | Math.min(dp(360), parentW-dp(52)), heightMeasureSpec);
		}
	}

	private class SelectableMergeRecyclerAdapter extends MergeRecyclerAdapter{
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			RecyclerView.ViewHolder holder=super.onCreateViewHolder(parent, viewType);
			if(holder instanceof SimpleListItemViewHolder ih){
				ih.itemView.setBackgroundResource(R.drawable.bg_selectable_list_item);
			}
			return holder;
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position){
			super.onBindViewHolder(holder, position);
			if(holder instanceof SimpleListItemViewHolder ih){
				ih.itemView.setSelected(ih.getItem()==selectedMenuItem);
			}
		}
	}
}
