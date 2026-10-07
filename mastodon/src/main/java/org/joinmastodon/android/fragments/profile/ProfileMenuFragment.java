package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.SavedPostsTimelineFragment;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.fragments.settings.SettingsMainFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.SettingsAccountListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ProfileMenuFragment extends BaseSettingsFragment<AccountSession>{
	private String accountID;
	private Account self;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		self=AccountSessionManager.get(accountID).self;

		ImageLoaderRequest req;
		if(self.avatar!=null)
			req=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? self.avatar : self.avatarStatic, V.dp(50), V.dp(50));
		else
			req=null;
		List<ListItem<AccountSession>> items=new java.util.ArrayList<>();
		items.add(new SettingsAccountListItem<>(self.displayName, self.getDisplayUsername(), req, this::onAccountClick, null, false));
		items.add(new ListItem<>(R.string.edit_profile, 0, R.drawable.ic_phosphor_user_light, this::onEditProfileClick));
		items.add(new ListItem<>(R.string.settings, 0, R.drawable.ic_phosphor_gear_light, this::onSettingsClick, 0, true));

		items.add(new ListItem<>(R.string.profile_collections, 0, R.drawable.ic_phosphor_circles_four_light, this::onCollectionsClick));
		items.add(new ListItem<>(R.string.your_favorites, 0, R.drawable.ic_phosphor_heart_light, this::onLikesClick));
		items.add(new ListItem<>(R.string.bookmarks, 0, R.drawable.ic_phosphor_bookmark_simple_light, this::onBookmarksClick, 0, true));

		items.add(new ListItem<>(R.string.profile_followers, 0, R.drawable.ic_phosphor_users_three_light, this::onFollowersClick));
		items.add(new ListItem<>(R.string.profile_following, 0, R.drawable.ic_phosphor_users_four_light, this::onFollowersClick));
		items.add(new ListItem<>(R.string.blocked_accounts, 0, R.drawable.ic_phosphor_prohibit_light, this::onBlockedAccountsClick, 0, true));

		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			if(session.getID().equals(accountID))
				continue;
			if(session.self.avatar!=null)
				req=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? session.self.avatar : session.self.avatarStatic, V.dp(50), V.dp(50));
			else
				req=null;
			items.add(new SettingsAccountListItem<>(session.self.displayName, session.getFullUsername(), req, this::onAnotherAccountClick, session, true));
		}

		items.add(new ListItem<>(R.string.add_account, 0, R.drawable.ic_phosphor_user_plus_light, this::onAddAccountClick, 0, true));
		items.add(new ListItem<>(R.string.log_out, 0, R.drawable.ic_phosphor_sign_out_light, this::onLogOutClick, 0, true));

		onDataLoaded(items);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	private void onAccountClick(ListItem<?> item){
		Bundle args=UiUtils.makeAccountArgs(accountID);
		args.putParcelable("profileAccount", Parcels.wrap(AccountSessionManager.getInstance().getAccount(accountID).self));
		Nav.go(getActivity(), ProfileFragment.class, args);
	}

	private void onEditProfileClick(ListItem<?> item){
		Bundle extras=UiUtils.makeAccountArgs(accountID);
//		extras.putInt("featuredTagCount", timelineFragment.getFeaturedHashtagCount());
		Nav.go(getActivity(), ProfileEditFragment.class, extras);
	}

	private void onSettingsClick(ListItem<?> item){
		Nav.go(getActivity(), SettingsMainFragment.class, UiUtils.makeAccountArgs(accountID));
	}

	private void onCollectionsClick(ListItem<?> item){
		Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
	}

	private void onLikesClick(ListItem<?> item){
		Bundle args=UiUtils.makeAccountArgs(accountID);
		args.putBoolean("isFavorites", true);
		Nav.go(getActivity(), SavedPostsTimelineFragment.class, args);
	}

	private void onBookmarksClick(ListItem<?> item){
		Bundle args=UiUtils.makeAccountArgs(accountID);
		args.putBoolean("isFavorites", false);
		Nav.go(getActivity(), SavedPostsTimelineFragment.class, args);
	}

	private void onFollowersClick(ListItem<?> item){
		Bundle args=UiUtils.makeAccountArgs(accountID);
		args.putParcelable("targetAccount", Parcels.wrap(self));
		Nav.go(getActivity(), FollowerListFragment.class, args);
	}

	private void onFollowingClick(ListItem<?> item){
		Bundle args=UiUtils.makeAccountArgs(accountID);
		args.putParcelable("targetAccount", Parcels.wrap(self));
		Nav.go(getActivity(), FollowingListFragment.class, args);
	}

	private void onBlockedAccountsClick(ListItem<?> item){
		Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
	}

	private void onAnotherAccountClick(ListItem<AccountSession> item){
		String id=item.parentObject.getID();
		if(AccountSessionManager.getInstance().tryGetAccount(id)!=null){
			AccountSessionManager.getInstance().setLastActiveAccountID(id);
			((MainActivity)getActivity()).restartHomeFragment();
		}
	}

	private void onAddAccountClick(ListItem<?> item){
		Nav.go(getActivity(), SplashFragment.class, null);
	}

	private void onLogOutClick(ListItem<?> item_){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new M3AlertDialogBuilder(getActivity())
				.setMessage(getString(R.string.confirm_log_out, session.getFullUsername()))
				.setPositiveButton(R.string.log_out, (dialog, which)->AccountSessionManager.get(accountID).logOut(getActivity(), ()->{
					((MainActivity)getActivity()).restartHomeFragment();
				}))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}
}
