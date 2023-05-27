package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class TopBarsScrollAwayLinearLayout extends LinearLayout{
	public TopBarsScrollAwayLinearLayout(Context context){
		this(context, null);
	}

	public TopBarsScrollAwayLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public TopBarsScrollAwayLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int topBarsHeight=0;
		for(int i=0;i<getChildCount()-1;i++){
			topBarsHeight+=getChildAt(i).getMeasuredHeight();
		}
		super.onMeasure(widthMeasureSpec, (MeasureSpec.getSize(heightMeasureSpec)+topBarsHeight) | MeasureSpec.EXACTLY);
	}
}
