package org.joinmastodon.android.fragments.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.V;

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
				new ListItem<>("Delete cached instance info", null, this::onDeleteInstanceInfoClick),
				new ListItem<>("View dynamic color values", null, this::onViewColorsClick)
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

	private void onViewColorsClick(ListItem<?> item){
		ArrayList<Pair<Integer, String>> attrs=new ArrayList<>();
		Field[] fields=R.attr.class.getFields();
		try{
			for(Field fld:fields){
				if(fld.getName().startsWith("color") && fld.getType().equals(int.class)){
					attrs.add(new Pair<>((Integer)fld.get(null), fld.getName()));
				}
			}
		}catch(IllegalAccessException x){
			Toast.makeText(getActivity(), x.toString(), Toast.LENGTH_SHORT).show();
			return;
		}

		class ColorsAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
			@NonNull
			@Override
			public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
				TextView view=new TextView(getActivity());
				int pad=V.dp(16);
				view.setPadding(pad, pad, pad, pad);
				view.setTextSize(14);
				view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				return new SimpleViewHolder(view);
			}

			@Override
			public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
				Pair<Integer, String> attr=attrs.get(position);
				TextView view=(TextView) holder.itemView;
				int color=UiUtils.getThemeColor(getActivity(), attr.first);
				view.setBackgroundColor(color);
				view.setText(String.format("%s\n#%06X", attr.second, (color & 0xFF000000) != 0xFF000000 ? color : (color & 0xFFFFFF)));
				view.setTextColor(new Palette.Swatch(color | 0xFF000000, 1).getBodyTextColor());
			}

			@Override
			public int getItemCount(){
				return attrs.size();
			}
		}

		RecyclerView rv=new RecyclerView(getActivity());
		rv.setLayoutManager(new LinearLayoutManager(getActivity()));
		rv.setAdapter(new ColorsAdapter());
		new M3AlertDialogBuilder(getActivity())
				.setTitle("Dynamic colors")
				.setView(rv)
				.setPositiveButton(R.string.ok, null)
				.show();
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
