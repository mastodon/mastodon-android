package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class HorizontalScrollViewThatRespectsMatchParent extends HorizontalScrollView{
	public HorizontalScrollViewThatRespectsMatchParent(Context context){
		super(context);
	}

	public HorizontalScrollViewThatRespectsMatchParent(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public HorizontalScrollViewThatRespectsMatchParent(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(getChildCount()==0)
			return;
		View child=getChildAt(0);
		ViewGroup.LayoutParams lp=child.getLayoutParams();
		if(lp.width==ViewGroup.LayoutParams.MATCH_PARENT){
			int hms=getChildMeasureSpec(heightMeasureSpec, getPaddingTop()+getPaddingBottom(), lp.height);
			child.measure(MeasureSpec.getSize(widthMeasureSpec) | MeasureSpec.EXACTLY, hms);
			setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
			return;
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
