package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

public class SizeListenerFrameLayout extends FrameLayout{
	private OnSizeChangedListener sizeListener;

	public SizeListenerFrameLayout(Context context){
		super(context);
	}

	public SizeListenerFrameLayout(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public SizeListenerFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		if(sizeListener!=null)
			sizeListener.onSizeChanged(w, h, oldw, oldh);
	}

	public void setSizeListener(OnSizeChangedListener sizeListener){
		this.sizeListener=sizeListener;
	}

	@FunctionalInterface
	public interface OnSizeChangedListener{
		void onSizeChanged(int w, int h, int oldw, int oldh);
	}
}
