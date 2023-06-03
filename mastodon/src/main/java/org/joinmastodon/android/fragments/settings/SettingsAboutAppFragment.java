package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SettingsAboutAppFragment extends BaseSettingsFragment<Void>{
	private ListItem<Void> mediaCacheItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.about_app, getString(R.string.app_name)));
		AccountSession s=AccountSessionManager.get(accountID);
		onDataLoaded(List.of(
				new ListItem<>(R.string.settings_even_more, 0, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/auth/edit")),
				new ListItem<>(R.string.settings_contribute, 0, ()->UiUtils.launchWebBrowser(getActivity(), getString(R.string.github_url))),
				new ListItem<>(R.string.settings_tos, 0, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/terms")),
				new ListItem<>(R.string.settings_privacy_policy, 0, ()->UiUtils.launchWebBrowser(getActivity(), getString(R.string.privacy_policy_url)), 0, true),
				mediaCacheItem=new ListItem<>(R.string.settings_clear_cache, 0, this::onClearMediaCacheClick)
		));

		updateMediaCacheItem();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(super.getAdapter());

		TextView versionInfo=new TextView(getActivity());
		versionInfo.setSingleLine();
		versionInfo.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(32)));
		versionInfo.setTextAppearance(R.style.m3_label_medium);
		versionInfo.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		versionInfo.setGravity(Gravity.CENTER);
		versionInfo.setText(getString(R.string.settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
		adapter.addAdapter(new SingleViewRecyclerAdapter(versionInfo));

		return adapter;
	}

	private void onClearMediaCacheClick(){
		MastodonAPIController.runInBackground(()->{
			Activity activity=getActivity();
			ImageCache.getInstance(getActivity()).clear();
			activity.runOnUiThread(()->{
				Toast.makeText(activity, R.string.media_cache_cleared, Toast.LENGTH_SHORT).show();
				updateMediaCacheItem();
			});
		});
	}

	private void updateMediaCacheItem(){
		long size=ImageCache.getInstance(getActivity()).getDiskCache().size();
		mediaCacheItem.subtitle=UiUtils.formatFileSize(getActivity(), size, false);
		mediaCacheItem.isEnabled=size>0;
		rebindItem(mediaCacheItem);
	}
}
