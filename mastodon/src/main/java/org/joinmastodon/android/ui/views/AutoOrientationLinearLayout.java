package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class AutoOrientationLinearLayout extends LinearLayout{
	public AutoOrientationLinearLayout(Context context){
		this(context, null);
	}

	public AutoOrientationLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public AutoOrientationLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int hPadding=getPaddingLeft()+getPaddingRight();
		int childrenTotalWidth=0;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
			childrenTotalWidth+=child.getMeasuredWidth();
		}
		if(childrenTotalWidth>MeasureSpec.getSize(widthMeasureSpec)-hPadding){
			setOrientation(VERTICAL);
		}else{
			setOrientation(HORIZONTAL);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
