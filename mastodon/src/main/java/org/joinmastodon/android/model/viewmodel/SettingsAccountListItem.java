package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import java.util.function.Consumer;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class SettingsAccountListItem<T> extends ListItem<T>{
	public ImageLoaderRequest avatar;

	public SettingsAccountListItem(String title, String subtitle, ImageLoaderRequest avatar, Consumer<SettingsAccountListItem<T>> onClick, T parentObject, boolean dividerAfter){
		super(title, subtitle, 0, (Consumer<ListItem<T>>)(Object)onClick, parentObject, 0, dividerAfter);
		this.avatar=avatar;
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_settings_account;
	}
}
