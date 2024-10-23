package org.joinmastodon.android.fragments.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.List;

import me.grishka.appkit.Nav;

public class SettingsDebugFragment extends BaseSettingsFragment<Void>{
	private CheckableListItem<Void> donationsStagingItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle("Debug settings");
		ListItem<Void> selfUpdateItem, resetUpdateItem;
		onDataLoaded(List.of(
				new ListItem<>("Re-register for FCM", null, this::onUpdatePushRegistrationClick),
				new ListItem<>("Test email confirmation flow", null, this::onTestEmailConfirmClick),
				selfUpdateItem=new ListItem<>("Force self-update", null, this::onForceSelfUpdateClick),
				resetUpdateItem=new ListItem<>("Reset self-updater", null, this::onResetUpdaterClick),
				new ListItem<>("Reset search info banners", null, this::onResetDiscoverBannersClick),
				new ListItem<>("Reset pre-reply sheets", null, this::onResetPreReplySheetsClick),
				new ListItem<>("Clear dismissed donation campaigns", null, this::onClearDismissedCampaignsClick),
				donationsStagingItem=new CheckableListItem<>("Use staging environment for donations", "Restart app to apply", CheckableListItem.Style.SWITCH, getPrefs().getBoolean("donationsStaging", false), this::toggleCheckableItem),
				new ListItem<>("Delete cached instance info", null, this::onDeleteInstanceInfoClick)
		));
		if(!GithubSelfUpdater.needSelfUpdating()){
			resetUpdateItem.isEnabled=selfUpdateItem.isEnabled=false;
			selfUpdateItem.subtitle="Self-updater is unavailable in this build flavor";
		}
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	public void onStop(){
		super.onStop();
		getPrefs().edit().putBoolean("donationsStaging", donationsStagingItem.checked).apply();
	}

	private void onUpdatePushRegistrationClick(ListItem<?> item){
		PushSubscriptionManager.resetLocalPreferences();
		PushSubscriptionManager.tryRegisterFCM();
	}

	private void onTestEmailConfirmClick(ListItem<?> item){
		AccountSession sess=AccountSessionManager.getInstance().getAccount(accountID);
		sess.activated=false;
		sess.activationInfo=new AccountActivationInfo("test@email", System.currentTimeMillis());
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putBoolean("debug", true);
		Nav.goClearingStack(getActivity(), AccountActivationFragment.class, args);
	}

	private void onForceSelfUpdateClick(ListItem<?> item){
		GithubSelfUpdater.forceUpdate=true;
		GithubSelfUpdater.getInstance().maybeCheckForUpdates();
		restartUI();
	}

	private void onResetUpdaterClick(ListItem<?> item){
		GithubSelfUpdater.getInstance().reset();
		restartUI();
	}

	private void onResetDiscoverBannersClick(ListItem<?> item){
		DiscoverInfoBannerHelper.reset();
		restartUI();
	}

	private void onResetPreReplySheetsClick(ListItem<?> item){
		GlobalUserPreferences.resetPreReplySheets();
		Toast.makeText(getActivity(), "Pre-reply sheets were reset", Toast.LENGTH_SHORT).show();
	}

	private void onClearDismissedCampaignsClick(ListItem<?> item){
		AccountSessionManager.getInstance().clearDismissedDonationCampaigns();
		Toast.makeText(getActivity(), "Dismissed campaigns cleared. Restart app to see your current campaign, if any", Toast.LENGTH_LONG).show();
	}

	private void onDeleteInstanceInfoClick(ListItem<?> item){
		AccountSessionManager.getInstance().clearInstanceInfo();
		Toast.makeText(getActivity(), "Instances removed from database", Toast.LENGTH_LONG).show();
	}

	private void restartUI(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.goClearingStack(getActivity(), HomeFragment.class, args);
	}

	private SharedPreferences getPrefs(){
		return getActivity().getSharedPreferences("debug", Context.MODE_PRIVATE);
	}
}
