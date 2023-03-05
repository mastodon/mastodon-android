package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FrameLayoutThatOnlyMeasuresFirstChild extends FrameLayout{
	public FrameLayoutThatOnlyMeasuresFirstChild(Context context){
		this(context, null);
	}

	public FrameLayoutThatOnlyMeasuresFirstChild(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public FrameLayoutThatOnlyMeasuresFirstChild(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(getChildCount()==0)
			return;
		View child0=getChildAt(0);
		measureChild(child0, widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(child0.getMeasuredWidth() | MeasureSpec.EXACTLY, child0.getMeasuredHeight() | MeasureSpec.EXACTLY);
	}
}
