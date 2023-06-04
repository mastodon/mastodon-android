package org.joinmastodon.android.fragments.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class SettingsMainFragment extends BaseSettingsFragment<Void>{
	private boolean loggedOut;
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
		setSubtitle(AccountSessionManager.get(accountID).getFullUsername());
		onDataLoaded(List.of(
				new ListItem<>(R.string.settings_behavior, 0, R.drawable.ic_settings_24px, this::onBehaviorClick),
				new ListItem<>(R.string.settings_display, 0, R.drawable.ic_style_24px, this::onDisplayClick),
				new ListItem<>(R.string.settings_filters, 0, R.drawable.ic_filter_alt_24px, this::onFiltersClick),
				new ListItem<>(R.string.settings_notifications, 0, R.drawable.ic_notifications_24px, this::onNotificationsClick),
				new ListItem<>(AccountSessionManager.get(accountID).domain, getString(R.string.settings_server_explanation), R.drawable.ic_dns_24px, this::onServerClick),
				new ListItem<>(getString(R.string.about_app, getString(R.string.app_name)), null, R.drawable.ic_info_24px, this::onAboutClick, null, 0, true),
				new ListItem<>(R.string.log_out, 0, R.drawable.ic_logout_24px, this::onLogOutClick, R.attr.colorM3Error, false)
		));

		if(BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals("appcenterPrivateBeta")){
			data.add(0, new ListItem<>("Debug settings", null, R.drawable.ic_settings_24px, ()->Nav.go(getActivity(), SettingsDebugFragment.class, makeFragmentArgs()), null, 0, true));
		}

		AccountSessionManager.get(accountID).reloadPreferences(null);
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
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		UiUtils.setToolbarWithSubtitleAppearance(getToolbar());
	}

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

	private Bundle makeFragmentArgs(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		return args;
	}

	private void onBehaviorClick(){
		Nav.go(getActivity(), SettingsBehaviorFragment.class, makeFragmentArgs());
	}

	private void onDisplayClick(){
		Nav.go(getActivity(), SettingsDisplayFragment.class, makeFragmentArgs());
	}

	private void onFiltersClick(){
		Nav.go(getActivity(), SettingsFiltersFragment.class, makeFragmentArgs());
	}

	private void onNotificationsClick(){
		Nav.go(getActivity(), SettingsNotificationsFragment.class, makeFragmentArgs());
	}

	private void onServerClick(){
		Nav.go(getActivity(), SettingsServerFragment.class, makeFragmentArgs());
	}

	private void onAboutClick(){
		Nav.go(getActivity(), SettingsAboutAppFragment.class, makeFragmentArgs());
	}

	private void onLogOutClick(){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new M3AlertDialogBuilder(getActivity())
				.setMessage(getString(R.string.confirm_log_out, session.getFullUsername()))
				.setPositiveButton(R.string.log_out, (dialog, which)->AccountSessionManager.get(accountID).logOut(getActivity(), ()->{
					loggedOut=true;
					getActivity().finish();
					Intent intent=new Intent(getActivity(), MainActivity.class);
					startActivity(intent);
				}))
				.setNegativeButton(R.string.cancel, null)
				.show();
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
