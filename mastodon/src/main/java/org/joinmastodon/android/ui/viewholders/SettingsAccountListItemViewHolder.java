package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.SettingsAccountListItem;
import org.joinmastodon.android.ui.OutlineProviders;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;

public class SettingsAccountListItemViewHolder extends ListItemViewHolder<SettingsAccountListItem<?>> implements ImageLoaderViewHolder{

	public SettingsAccountListItemViewHolder(Context context, ViewGroup parent){
		super(context, R.layout.item_generic_list, parent);
		icon.setOutlineProvider(OutlineProviders.OVAL);
		icon.setClipToOutline(true);
		icon.setImageTintList(null);
	}

	@Override
	protected void bindIcon(SettingsAccountListItem<?> item){}

	@Override
	public void setImage(int index, Drawable image){
		icon.setImageDrawable(image);
	}

	@Override
	public void clearImage(int index){
		icon.setImageResource(R.drawable.image_placeholder);
	}
}
