package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;

import java.util.List;
import java.util.Locale;

public class SettingsBehaviorFragment extends BaseSettingsFragment<Void>{
	private ListItem<Void> languageItem;
	private CheckableListItem<Void> altTextItem, playGifsItem, customTabsItem, confirmUnfollowItem, confirmBoostItem, confirmDeleteItem;
	private Locale postLanguage;
	private ComposeLanguageAlertViewController.SelectedOption newPostLanguage;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_behavior);

		AccountSession s=AccountSessionManager.get(accountID);
		if(s.preferences!=null && s.preferences.postingDefaultLanguage!=null){
			postLanguage=Locale.forLanguageTag(s.preferences.postingDefaultLanguage);
		}

		onDataLoaded(List.of(
				languageItem=new ListItem<>(getString(R.string.default_post_language), postLanguage!=null ? postLanguage.getDisplayName(Locale.getDefault()) : null, R.drawable.ic_language_24px, this::onDefaultLanguageClick),
				altTextItem=new CheckableListItem<>(R.string.settings_alt_text_reminders, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.altTextReminders, R.drawable.ic_alt_24px, ()->toggleCheckableItem(altTextItem)),
				playGifsItem=new CheckableListItem<>(R.string.settings_gif, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.playGifs, R.drawable.ic_animation_24px, ()->toggleCheckableItem(playGifsItem)),
				customTabsItem=new CheckableListItem<>(R.string.settings_custom_tabs, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.useCustomTabs, R.drawable.ic_open_in_browser_24px, ()->toggleCheckableItem(customTabsItem)),
				confirmUnfollowItem=new CheckableListItem<>(R.string.settings_confirm_unfollow, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmUnfollow, R.drawable.ic_person_remove_24px, ()->toggleCheckableItem(confirmUnfollowItem)),
				confirmBoostItem=new CheckableListItem<>(R.string.settings_confirm_boost, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmBoost, R.drawable.ic_repeat_24px, ()->toggleCheckableItem(confirmBoostItem)),
				confirmDeleteItem=new CheckableListItem<>(R.string.settings_confirm_delete_post, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmDeletePost, R.drawable.ic_delete_24px, ()->toggleCheckableItem(confirmDeleteItem))
		));
	}

	@Override
	protected void doLoadData(int offset, int count){}

	private void onDefaultLanguageClick(){
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), null, new ComposeLanguageAlertViewController.SelectedOption(-1, postLanguage), null);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.default_post_language)
				.setView(vc.getView())
				.setPositiveButton(R.string.ok, (dlg, which)->{
					ComposeLanguageAlertViewController.SelectedOption opt=vc.getSelectedOption();
					if(!opt.locale.equals(postLanguage)){
						newPostLanguage=opt;
						languageItem.subtitle=newPostLanguage.locale.getDisplayLanguage(Locale.getDefault());
						rebindItem(languageItem);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.playGifs=playGifsItem.checked;
		GlobalUserPreferences.useCustomTabs=customTabsItem.checked;
		GlobalUserPreferences.altTextReminders=altTextItem.checked;
		GlobalUserPreferences.confirmUnfollow=customTabsItem.checked;
		GlobalUserPreferences.confirmBoost=confirmBoostItem.checked;
		GlobalUserPreferences.confirmDeletePost=confirmDeleteItem.checked;
		GlobalUserPreferences.save();
		if(newPostLanguage!=null){
			AccountSession s=AccountSessionManager.get(accountID);
			if(s.preferences==null)
				s.preferences=new Preferences();
			s.preferences.postingDefaultLanguage=newPostLanguage.locale.toLanguageTag();
			s.savePreferencesLater();
		}
	}
}
