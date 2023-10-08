package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import java.util.List;
import java.util.function.Consumer;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class AvatarPileListItem<T> extends ListItem<T>{
	public List<ImageLoaderRequest> avatars;

	public AvatarPileListItem(String title, String subtitle, List<ImageLoaderRequest> avatars, int iconRes, Consumer<AvatarPileListItem<T>> onClick, T parentObject, boolean dividerAfter){
		super(title, subtitle, iconRes, (Consumer<ListItem<T>>)(Object)onClick, parentObject, 0, dividerAfter);
		this.avatars=avatars;
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_avatar_pile;
	}
}
