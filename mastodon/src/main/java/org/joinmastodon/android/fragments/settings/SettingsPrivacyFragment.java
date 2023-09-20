package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;

import java.util.List;

public class SettingsPrivacyFragment extends BaseSettingsFragment<Void>{
	private CheckableListItem<Void> discoverableItem, indexableItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_privacy);
		Account self=AccountSessionManager.get(accountID).self;
		onDataLoaded(List.of(
			discoverableItem=new CheckableListItem<>(R.string.settings_discoverable, 0, CheckableListItem.Style.SWITCH, self.discoverable, R.drawable.ic_thumbs_up_down_24px, ()->toggleCheckableItem(discoverableItem)),
			indexableItem=new CheckableListItem<>(R.string.settings_indexable, 0, CheckableListItem.Style.SWITCH, self.indexable, R.drawable.ic_search_24px, ()->toggleCheckableItem(indexableItem))
		));
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	public void onPause(){
		super.onPause();
		Account self=AccountSessionManager.get(accountID).self;
		if(self.discoverable!=discoverableItem.checked || self.indexable!=indexableItem.checked){
			self.discoverable=discoverableItem.checked;
			self.indexable=indexableItem.checked;
			AccountSessionManager.get(accountID).savePreferencesLater();
		}
	}
}
