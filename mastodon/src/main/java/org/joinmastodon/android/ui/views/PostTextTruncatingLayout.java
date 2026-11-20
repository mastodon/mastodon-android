package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class PostTextTruncatingLayout extends LinearLayout{

	public PostTextTruncatingLayout(Context context){
		this(context, null);
	}

	public PostTextTruncatingLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public PostTextTruncatingLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		LinkedTextView textView=(LinkedTextView) getChildAt(0);
		View button=getChildAt(1);
		button.setVisibility(View.GONE);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(textView.isTruncatedVertically()){
			button.setVisibility(View.VISIBLE);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}
