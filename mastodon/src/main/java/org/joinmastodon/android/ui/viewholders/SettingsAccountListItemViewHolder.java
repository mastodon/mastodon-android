package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.SettingsAccountListItem;
import org.joinmastodon.android.ui.OutlineProviders;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.V;

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

	@Override
	public void onBind(SettingsAccountListItem<?> item){
		super.onBind(item);
		if(item.emphasized){
			itemView.setPadding(itemView.getPaddingLeft(), 0, itemView.getPaddingRight(), 0);
			itemView.setBackgroundResource(R.drawable.bg_emphasized_list_item);
			view.setMinimumHeight(V.dp(56));
		}else{
			itemView.setPadding(itemView.getPaddingLeft(), V.dp(12), itemView.getPaddingRight(), V.dp(12));
			itemView.setBackground(null);
		}
	}
}
