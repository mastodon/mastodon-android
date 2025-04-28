package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;

public class TopBarsScrollAwayLinearLayout extends LinearLayout{
	private int topBarsCount;

	public TopBarsScrollAwayLinearLayout(Context context){
		this(context, null);
	}

	public TopBarsScrollAwayLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public TopBarsScrollAwayLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.TopBarsScrollAwayLinearLayout);
		topBarsCount=ta.getInteger(R.styleable.TopBarsScrollAwayLinearLayout_topBarsCount, -1);
		ta.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(getOrientation()!=VERTICAL)
			throw new IllegalStateException("This LinearLayout only supports vertical orientation");
		int topBarsHeight=0;
		int count=topBarsCount==-1 ? (getChildCount()-1) : topBarsCount;
		for(int i=0;i<count;i++){
			View child=getChildAt(i);
			if(child.getVisibility()==GONE)
				continue;
			measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
			topBarsHeight+=child.getMeasuredHeight();
			if(child.getLayoutParams() instanceof MarginLayoutParams mlp)
				topBarsHeight+=mlp.topMargin+mlp.bottomMargin;
		}
		super.onMeasure(widthMeasureSpec, (MeasureSpec.getSize(heightMeasureSpec)+topBarsHeight) | MeasureSpec.EXACTLY);
	}

	public int getTopBarsCount(){
		return topBarsCount;
	}

	public void setTopBarsCount(int topBarsCount){
		this.topBarsCount=topBarsCount;
	}
}
