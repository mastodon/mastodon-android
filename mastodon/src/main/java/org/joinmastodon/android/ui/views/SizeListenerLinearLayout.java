package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class SizeListenerLinearLayout extends LinearLayout{
	private OnSizeChangedListener sizeListener;
	private int oldWidth, oldHeight;

	public SizeListenerLinearLayout(Context context){
		super(context);
	}

	public SizeListenerLinearLayout(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public SizeListenerLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);
		if(changed && sizeListener!=null){
			int w=r-l;
			int h=b-t;
			sizeListener.onSizeChanged(w, h, oldWidth, oldHeight);
			oldWidth=w;
			oldHeight=h;
		}
	}

	public void setSizeListener(OnSizeChangedListener sizeListener){
		this.sizeListener=sizeListener;
	}

	@FunctionalInterface
	public interface OnSizeChangedListener{
		void onSizeChanged(int w, int h, int oldw, int oldh);
	}
}
