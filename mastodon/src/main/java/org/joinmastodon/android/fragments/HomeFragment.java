package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.discover.DiscoverFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.views.TabBar;
import org.parceler.Parcels;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment implements OnBackPressedListener{
	private FragmentRootLinearLayout content;
	private HomeTimelineFragment homeTimelineFragment;
	private NotificationsFragment notificationsFragment;
	private DiscoverFragment searchFragment;
	private ProfileFragment profileFragment;
	private TabBar tabBar;
	private View tabBarWrap;
	private ImageView tabBarAvatar;
	@IdRes
	private int currentTab=R.id.tab_home;

	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		Bundle args=new Bundle();
		args.putString("account", accountID);
		homeTimelineFragment=new HomeTimelineFragment();
		homeTimelineFragment.setArguments(args);
		args=new Bundle(args);
		args.putBoolean("noAutoLoad", true);
		searchFragment=new DiscoverFragment();
		searchFragment.setArguments(args);
		notificationsFragment=new NotificationsFragment();
		notificationsFragment.setArguments(args);
		args=new Bundle(args);
		args.putParcelable("profileAccount", Parcels.wrap(AccountSessionManager.getInstance().getAccount(accountID).self));
		args.putBoolean("noAutoLoad", true);
		profileFragment=new ProfileFragment();
		profileFragment.setArguments(args);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		FrameLayout fragmentContainer=new FrameLayout(getActivity());
		fragmentContainer.setId(R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.tab_bar, content);
		tabBar=content.findViewById(R.id.tabbar);
		tabBar.setListener(this::onTabSelected);
		tabBarWrap=content.findViewById(R.id.tabbar_wrap);

		tabBarAvatar=tabBar.findViewById(R.id.tab_profile_ava);
		tabBarAvatar.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setOval(0, 0, view.getWidth(), view.getHeight());
			}
		});
		tabBarAvatar.setClipToOutline(true);
		Account self=AccountSessionManager.getInstance().getAccount(accountID).self;
		ViewImageLoader.load(tabBarAvatar, null, new UrlImageLoaderRequest(self.avatar, V.dp(28), V.dp(28)));

		if(savedInstanceState==null){
			getChildFragmentManager().beginTransaction()
					.add(R.id.fragment_wrap, homeTimelineFragment)
					.add(R.id.fragment_wrap, searchFragment).hide(searchFragment)
					.add(R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.add(R.id.fragment_wrap, profileFragment).hide(profileFragment)
					.commit();
		}else{
			tabBar.selectTab(currentTab);
		}

		return content;
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		fragmentForTab(currentTab).onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return currentTab!=R.id.tab_profile && (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES;
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			tabBarWrap.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnlyInsets=insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);
		homeTimelineFragment.onApplyWindowInsets(topOnlyInsets);
		searchFragment.onApplyWindowInsets(topOnlyInsets);
		notificationsFragment.onApplyWindowInsets(topOnlyInsets);
		profileFragment.onApplyWindowInsets(topOnlyInsets);
	}

	private Fragment fragmentForTab(@IdRes int tab){
		if(tab==R.id.tab_home){
			return homeTimelineFragment;
		}else if(tab==R.id.tab_search){
			return searchFragment;
		}else if(tab==R.id.tab_notifications){
			return notificationsFragment;
		}else if(tab==R.id.tab_profile){
			return profileFragment;
		}
		throw new IllegalArgumentException();
	}

	private void onTabSelected(@IdRes int tab){
		Fragment newFragment=fragmentForTab(tab);
		if(tab==currentTab){
			if(newFragment instanceof ScrollableToTop)
				((ScrollableToTop) newFragment).scrollToTop();
			return;
		}
		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(newFragment).commit();
		if(newFragment instanceof LoaderFragment){
			LoaderFragment lf=(LoaderFragment) newFragment;
			if(!lf.loaded && !lf.dataLoading)
				lf.loadData();
		}else if(newFragment instanceof DiscoverFragment){
			((DiscoverFragment) newFragment).loadData();
		}else if(newFragment instanceof NotificationsFragment){
			((NotificationsFragment) newFragment).loadData();
			// TODO make an interface?
		}
		currentTab=tab;
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	@Override
	public boolean onBackPressed(){
		if(currentTab==R.id.tab_profile)
			return profileFragment.onBackPressed();
		return false;
	}
}
