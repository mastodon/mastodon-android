package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import me.grishka.appkit.views.UsableRecyclerView;

public class PostListRecyclerView extends UsableRecyclerView{
	public PostListRecyclerView(Context context){
		this(context, null);
	}

	public PostListRecyclerView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public PostListRecyclerView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	public boolean drawChild(Canvas canvas, View child, long drawingTime){
		if(getChildViewHolder(child) instanceof StatusDisplayItem.Holder<?> ih && ((StatusDisplayItem)ih.getItem()).isInsideCW){
			return false;
		}
		return super.drawChild(canvas, child, drawingTime);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e){
		if(e.getAction()==MotionEvent.ACTION_DOWN){
			View target=isIncludeMarginsInItemHitbox() ? findChildViewUnderWithMargins(e.getX(), e.getY()) : findChildViewUnder(e.getX(), e.getY());
			if(target!=null && getChildViewHolder(target) instanceof StatusDisplayItem.Holder<?> ih && ((StatusDisplayItem)ih.getItem()).isInsideCW){
				// Don't let touch events get to a child view hidden behind a CW
				return true;
			}
		}
		return super.onInterceptTouchEvent(e);
	}
}
