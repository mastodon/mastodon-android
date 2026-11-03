package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.fragments.SavedPostsTimelineFragment;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.fragments.account_list.BlockedAccountsListFragment;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.account_list.MutedAccountsListFragment;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.fragments.settings.SettingsMainFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.SettingsAccountListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ProfileMenuFragment extends BaseSettingsFragment<AccountSession>{
	private String accountID;
	private Account self;
	private ListItem<AccountSession> settingsItem;

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
		SettingsAccountListItem<AccountSession> profileItem=new SettingsAccountListItem<>(self.displayName, self.getDisplayUsername(), req, this::onAccountClick, null, false);
		profileItem.emphasized=true;
		items.add(profileItem);
		items.add(new ListItem<>(R.string.edit_profile, 0, R.drawable.ic_phosphor_user_light, this::onEditProfileClick));
		items.add(settingsItem=new ListItem<>(R.string.settings, 0, R.drawable.ic_phosphor_gear_light, this::onSettingsClick, 0, true));

		items.add(new ListItem<>(R.string.profile_collections, 0, R.drawable.ic_phosphor_circles_four_light, this::onCollectionsClick));
		items.add(new ListItem<>(R.string.your_favorites, 0, R.drawable.ic_phosphor_heart_light, this::onLikesClick));
		items.add(new ListItem<>(R.string.bookmarks, 0, R.drawable.ic_phosphor_bookmark_simple_light, this::onBookmarksClick, 0, true));

		items.add(new ListItem<>(R.string.profile_followers, 0, R.drawable.ic_phosphor_hand_waving_light, this::onFollowersClick));
		items.add(new ListItem<>(R.string.profile_following, 0, R.drawable.ic_phosphor_users_three_light, this::onFollowingClick));
		items.add(new ListItem<>(R.string.muted_accounts, 0, R.drawable.ic_phosphor_bell_slash_light, this::onMutedAccountsClick));
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

		GithubSelfUpdater.UpdateState state=GithubSelfUpdater.UpdateState.NO_UPDATE;
		GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
		if(updater!=null)
			state=updater.getState();
		updateUpdateState(state);

		onDataLoaded(items);

		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setVisibility(View.GONE);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.setPadding(list.getPaddingLeft(), V.dp(16), list.getPaddingRight(), list.getPaddingBottom());
	}

	private void updateUpdateState(GithubSelfUpdater.UpdateState state){
		if(state!=GithubSelfUpdater.UpdateState.NO_UPDATE && state!=GithubSelfUpdater.UpdateState.CHECKING){
			settingsItem.subtitleRes=R.string.app_update_available;
			rebindItem(settingsItem);
		}
	}

	@Subscribe
	public void onSelfUpdateStateChanged(SelfUpdateStateChangedEvent ev){
		updateUpdateState(ev.state);
	}

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
		Nav.go(getActivity(), BlockedAccountsListFragment.class, UiUtils.makeAccountArgs(accountID));
	}

	private void onMutedAccountsClick(ListItem<?> item){
		Nav.go(getActivity(), MutedAccountsListFragment.class, UiUtils.makeAccountArgs(accountID));
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
