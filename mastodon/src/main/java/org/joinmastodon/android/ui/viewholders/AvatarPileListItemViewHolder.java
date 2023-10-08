package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.AvatarPileListItem;
import org.joinmastodon.android.ui.views.AvatarPileView;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.V;

public class AvatarPileListItemViewHolder extends ListItemViewHolder<AvatarPileListItem<?>> implements ImageLoaderViewHolder{
	private final AvatarPileView pile;

	public AvatarPileListItemViewHolder(Context context, ViewGroup parent){
		super(context, R.layout.item_generic_list, parent);
		pile=new AvatarPileView(context);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		lp.topMargin=lp.bottomMargin=V.dp(-8);
		view.addView(pile, lp);
		view.setClipToPadding(false);
	}

	@Override
	public void onBind(AvatarPileListItem<?> item){
		super.onBind(item);
		pile.setVisibleAvatarCount(item.avatars.size());
	}

	@Override
	public void setImage(int index, Drawable image){
		pile.avatars[index].setImageDrawable(image);
	}

	@Override
	public void clearImage(int index){
		pile.avatars[index].setImageResource(R.drawable.image_placeholder);
	}
}
