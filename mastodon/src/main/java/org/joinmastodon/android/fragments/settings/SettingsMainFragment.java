package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.catalog.GetDonationCampaigns;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.model.donations.DonationCampaign;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.sheets.DonationSheet;
import org.joinmastodon.android.ui.sheets.DonationSuccessfulSheet;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class SettingsMainFragment extends BaseSettingsFragment<Void>{
	private static final int DONATION_RESULT=433;

	private boolean loggedOut;
	private HideableSingleViewRecyclerAdapter bannerAdapter;
	private Button updateButton1, updateButton2;
	private TextView updateText;
	private DonationSheet donationSheet;
	private Runnable updateDownloadProgressUpdater=new Runnable(){
		@Override
		public void run(){
			GithubSelfUpdater.UpdateState state=GithubSelfUpdater.getInstance().getState();
			if(state==GithubSelfUpdater.UpdateState.DOWNLOADING){
				updateButton1.setText(getString(R.string.downloading_update, Math.round(GithubSelfUpdater.getInstance().getDownloadProgress()*100f)));
				list.postDelayed(this, 250);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings);
		setSubtitle(AccountSessionManager.get(accountID).getFullUsername());
		ArrayList<ListItem<Void>> items=new ArrayList<>();
		if(BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals("appcenterPrivateBeta")){
			items.add(new ListItem<>("Debug settings", null, R.drawable.ic_settings_24px, i->Nav.go(getActivity(), SettingsDebugFragment.class, makeFragmentArgs()), null, 0, true));
		}
		items.addAll(List.of(
				new ListItem<>(R.string.settings_behavior, 0, R.drawable.ic_settings_24px, this::onBehaviorClick),
				new ListItem<>(R.string.settings_display, 0, R.drawable.ic_style_24px, this::onDisplayClick),
				new ListItem<>(R.string.settings_privacy, 0, R.drawable.ic_privacy_tip_24px, this::onPrivacyClick),
				new ListItem<>(R.string.settings_filters, 0, R.drawable.ic_filter_alt_24px, this::onFiltersClick),
				new ListItem<>(R.string.settings_notifications, 0, R.drawable.ic_notifications_24px, this::onNotificationsClick),
				new ListItem<>(AccountSessionManager.get(accountID).domain, getString(R.string.settings_server_explanation), R.drawable.ic_dns_24px, this::onServerClick),
				new ListItem<>(getString(R.string.about_app, getString(R.string.app_name)), null, R.drawable.ic_info_24px, this::onAboutClick, null, 0, true)
		));
		if(AccountSessionManager.get(accountID).isEligibleForDonations()){
			items.add(new ListItem<>(R.string.settings_donate, 0, R.drawable.ic_volunteer_activism_24px, this::onDonateClick));
			items.add(new ListItem<>(R.string.settings_manage_donations, 0, R.drawable.ic_settings_heart_24px, this::onManageDonationClick, 0, true));
		}
		items.add(new ListItem<>(R.string.manage_accounts, 0, R.drawable.ic_switch_account_24px, this::onManageAccountsClick));
		items.add(new ListItem<>(R.string.log_out, 0, R.drawable.ic_logout_24px, this::onLogOutClick, R.attr.colorM3Error, false));
		onDataLoaded(items);

		AccountSession session=AccountSessionManager.get(accountID);
		session.reloadPreferences(null);
		session.updateAccountInfo();
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(!loggedOut)
			AccountSessionManager.get(accountID).savePreferencesIfPending();
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		View banner=getActivity().getLayoutInflater().inflate(R.layout.item_settings_banner, list, false);
		updateText=banner.findViewById(R.id.text);
		TextView bannerTitle=banner.findViewById(R.id.title);
		ImageView bannerIcon=banner.findViewById(R.id.icon);
		updateButton1=banner.findViewById(R.id.button);
		updateButton2=banner.findViewById(R.id.button2);
		bannerAdapter=new HideableSingleViewRecyclerAdapter(banner);
		bannerAdapter.setVisible(false);
		updateButton1.setOnClickListener(this::onUpdateButtonClick);
		updateButton2.setOnClickListener(this::onUpdateButtonClick);

		bannerTitle.setText(R.string.app_update_ready);
		bannerIcon.setImageResource(R.drawable.ic_apk_install_24px);

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(bannerAdapter);
		adapter.addAdapter(super.getAdapter());
		return adapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(GithubSelfUpdater.needSelfUpdating()){
			updateUpdateBanner();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==DONATION_RESULT){
			if(donationSheet!=null)
				donationSheet.dismissWithoutAnimation();
			if(resultCode==Activity.RESULT_OK){
				new DonationSuccessfulSheet(getActivity(), accountID, data.getStringExtra("postText")).showWithoutAnimation();
			}
		}
	}

	private Bundle makeFragmentArgs(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		return args;
	}

	private void onBehaviorClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsBehaviorFragment.class, makeFragmentArgs());
	}

	private void onDisplayClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsDisplayFragment.class, makeFragmentArgs());
	}

	private void onPrivacyClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsPrivacyFragment.class, makeFragmentArgs());
	}

	private void onFiltersClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsFiltersFragment.class, makeFragmentArgs());
	}

	private void onNotificationsClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsNotificationsFragment.class, makeFragmentArgs());
	}

	private void onServerClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsServerFragment.class, makeFragmentArgs());
	}

	private void onAboutClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsAboutAppFragment.class, makeFragmentArgs());
	}

	private void onManageAccountsClick(ListItem<?> item){
		new AccountSwitcherSheet(getActivity(), null).setOnLoggedOutCallback(()->loggedOut=true).show();
	}

	private void onLogOutClick(ListItem<?> item_){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new M3AlertDialogBuilder(getActivity())
				.setMessage(getString(R.string.confirm_log_out, session.getFullUsername()))
				.setPositiveButton(R.string.log_out, (dialog, which)->AccountSessionManager.get(accountID).logOut(getActivity(), ()->{
					loggedOut=true;
					((MainActivity)getActivity()).restartHomeFragment();
				}))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onDonateClick(ListItem<?> item){
		GetDonationCampaigns req=new GetDonationCampaigns(Locale.getDefault().toLanguageTag().replace('-', '_'), String.valueOf(AccountSessionManager.get(accountID).getDonationSeed()), null);
		if(BuildConfig.DEBUG && getActivity().getSharedPreferences("debug", Context.MODE_PRIVATE).getBoolean("donationsStaging", false)){
			req.setStaging(true);
		}
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(DonationCampaign result){
						Activity activity=getActivity();
						if(activity==null)
							return;
						if(result==null){
							Toast.makeText(activity, "No campaign available (server misconfiguration?)", Toast.LENGTH_SHORT).show();
							return;
						}
						donationSheet=new DonationSheet(getActivity(), result, accountID, intent->startActivityForResult(intent, DONATION_RESULT));
						donationSheet.setOnDismissListener(dialog->donationSheet=null);
						donationSheet.show();
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, true)
				.execNoAuth("");
	}

	private void onManageDonationClick(ListItem<?> item){
		UiUtils.launchWebBrowser(getActivity(), "https://sponsor.staging.joinmastodon.org/donate/manage");
	}

	@Subscribe
	public void onSelfUpdateStateChanged(SelfUpdateStateChangedEvent ev){
		updateUpdateBanner();
	}

	private void updateUpdateBanner(){
		GithubSelfUpdater.UpdateState state=GithubSelfUpdater.getInstance().getState();
		if(state==GithubSelfUpdater.UpdateState.NO_UPDATE || state==GithubSelfUpdater.UpdateState.CHECKING){
			bannerAdapter.setVisible(false);
		}else{
			bannerAdapter.setVisible(true);
			updateText.setText(getString(R.string.app_update_version, GithubSelfUpdater.getInstance().getUpdateInfo().version));
			if(state==GithubSelfUpdater.UpdateState.UPDATE_AVAILABLE){
				updateButton2.setVisibility(View.GONE);
				updateButton1.setEnabled(true);
				updateButton1.setText(getString(R.string.download_update, UiUtils.formatFileSize(getActivity(), GithubSelfUpdater.getInstance().getUpdateInfo().size, true)));
			}else if(state==GithubSelfUpdater.UpdateState.DOWNLOADING){
				updateButton2.setVisibility(View.VISIBLE);
				updateButton2.setText(R.string.cancel);
				updateButton1.setEnabled(false);
				list.removeCallbacks(updateDownloadProgressUpdater);
				updateDownloadProgressUpdater.run();
			}else if(state==GithubSelfUpdater.UpdateState.DOWNLOADED){
				updateButton2.setVisibility(View.GONE);
				updateButton1.setEnabled(true);
				updateButton1.setText(R.string.install_update);
			}
		}
	}

	private void onUpdateButtonClick(View v){
		if(v.getId()==R.id.button){
			GithubSelfUpdater.UpdateState state=GithubSelfUpdater.getInstance().getState();
			if(state==GithubSelfUpdater.UpdateState.UPDATE_AVAILABLE){
				GithubSelfUpdater.getInstance().downloadUpdate();
			}else if(state==GithubSelfUpdater.UpdateState.DOWNLOADED){
				GithubSelfUpdater.getInstance().installUpdate(getActivity());
			}
		}else if(v.getId()==R.id.button2){
			GithubSelfUpdater.getInstance().cancelDownload();
		}
	}
}
