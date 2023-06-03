package org.joinmastodon.android.utils;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ViewImageLoader;

public class ViewImageLoaderHolderTarget implements ViewImageLoader.Target{
	private final ImageLoaderViewHolder holder;
	private final int imageIndex;

	public ViewImageLoaderHolderTarget(ImageLoaderViewHolder holder, int imageIndex){
		this.holder=holder;
		this.imageIndex=imageIndex;
	}

	@Override
	public void setImageDrawable(Drawable d){
		if(d==null)
			holder.clearImage(imageIndex);
		else
			holder.setImage(imageIndex, d);
	}

	@Override
	public View getView(){
		return ((RecyclerView.ViewHolder)holder).itemView;
	}
}
