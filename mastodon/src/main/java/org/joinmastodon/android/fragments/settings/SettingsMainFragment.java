package org.joinmastodon.android.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.SectionHeaderListItem;
import org.joinmastodon.android.model.viewmodel.SettingsAccountListItem;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SettingsMainFragment extends BaseSettingsFragment<Object>{

	private HideableSingleViewRecyclerAdapter bannerAdapter;
	private Button updateButton1, updateButton2;
	private TextView updateText;
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
		ArrayList<ListItem<?>> items=new ArrayList<>();
		items.add(new SectionHeaderListItem(R.string.settings_accounts));
		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			ImageLoaderRequest req;
			if(session.self.avatar!=null)
				req=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? session.self.avatar : session.self.avatarStatic, V.dp(50), V.dp(50));
			else
				req=null;
			items.add(new SettingsAccountListItem<>(session.getFullUsername(), null, req, this::onAccountClick, session, false));
		}

		items.addAll(List.of(
				new ListItem<>(R.string.settings_add_account, 0, R.drawable.ic_add_24px, this::onAddAccountClick),

				new SectionHeaderListItem(R.string.settings_app_settings),
				new ListItem<>(R.string.settings_behavior, 0, R.drawable.ic_tune_24px, this::onBehaviorClick),
				new ListItem<>(R.string.settings_display, 0, R.drawable.ic_style_24px, this::onDisplayClick)

		));
		if(AccountSessionManager.get(accountID).isEligibleForDonations()){
			items.add(new ListItem<>(R.string.settings_manage_donations, 0, R.drawable.ic_settings_heart_24px, this::onManageDonationClick));
		}
		items.add(new ListItem<>(getString(R.string.about_app, getString(R.string.app_name)), null, R.drawable.ic_info_24px, this::onAboutClick, null));
		if(BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals("appcenterPrivateBeta")){
			items.add(new ListItem<>("Debug settings", null, R.drawable.ic_bug_report_24px, i->Nav.go(getActivity(), SettingsDebugFragment.class, makeFragmentArgs()), null));
		}

		//noinspection unchecked
		onDataLoaded((List<ListItem<Object>>)(Object)items);

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

	private Bundle makeFragmentArgs(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		return args;
	}

	private void onAccountClick(SettingsAccountListItem<AccountSession> item){
		Bundle args=new Bundle();
		args.putString("account", item.parentObject.getID());
		Nav.go(getActivity(), SettingsAccountFragment.class, args);
	}

	private void onAddAccountClick(ListItem<?> item_){
		Nav.go(getActivity(), SplashFragment.class, null);
	}

	private void onBehaviorClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsBehaviorFragment.class, makeFragmentArgs());
	}

	private void onDisplayClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsDisplayFragment.class, makeFragmentArgs());
	}

	private void onAboutClick(ListItem<?> item_){
		Nav.go(getActivity(), SettingsAboutAppFragment.class, makeFragmentArgs());
	}

	private boolean useStagingEnvironmentForDonations(){
		return (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals("appcenterPrivateBeta")) && getActivity().getSharedPreferences("debug", Context.MODE_PRIVATE).getBoolean("donationsStaging", false);
	}

	private void onManageDonationClick(ListItem<?> item){
		UiUtils.launchWebBrowser(getActivity(), useStagingEnvironmentForDonations() ? "https://sponsor.staging.joinmastodon.org/donate/manage" : "https://sponsor.joinmastodon.org/donate/manage");
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
