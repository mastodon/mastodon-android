package org.joinmastodon.android.ui.views;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.List;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CustomViewHelper;

/**
 * Intended for displaying a bunch of StatusDisplayItems in arbitrary places outside of a BaseStatusListFragment.
 */
public class StatusView extends LinearLayout implements CustomViewHelper{
	private final Fragment parentFragment;

	public StatusView(Context context, Fragment parentFragment){
		super(context);
		this.parentFragment=parentFragment;
		setOrientation(VERTICAL);
		setPadding(0, 0, 0, dp(6));
		setFocusable(false);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev){
		return false; // nope.
	}

	public void addItems(List<StatusDisplayItem> items){
		for(StatusDisplayItem item:items){
			//noinspection unchecked
			BindableViewHolder<StatusDisplayItem> holder=(BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(item.getType(), (Activity)getContext(), this, parentFragment);
			holder.bind(item);
			LayoutParams lp=new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.leftMargin=lp.rightMargin=dp(-4);
			if(item.getType()==StatusDisplayItem.Type.NESTED_QUOTE)
				lp.bottomMargin=dp(6);
			addView(holder.itemView, lp);
			for(int i=0;i<item.getImageCount();i++){
				ImageLoaderRequest req=item.getImageRequest(i);
				if(req==null)
					continue;
				final int _i=i;
				ViewImageLoader.load(new ViewImageLoader.Target(){
					@Override
					public void setImageDrawable(Drawable d){
						if(d==null)
							((ImageLoaderViewHolder)holder).clearImage(_i);
						else
							((ImageLoaderViewHolder)holder).setImage(_i, d);
					}

					@Override
					public View getView(){
						return holder.itemView;
					}
				}, null, req, false, true);
			}
		}
	}
}
