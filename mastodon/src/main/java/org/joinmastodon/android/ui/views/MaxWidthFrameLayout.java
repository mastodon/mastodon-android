package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.joinmastodon.android.R;

public class MaxWidthFrameLayout extends FrameLayout{
	private int maxWidth;

	public MaxWidthFrameLayout(Context context){
		this(context, null);
	}

	public MaxWidthFrameLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public MaxWidthFrameLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.MaxWidthFrameLayout);
		maxWidth=ta.getDimensionPixelSize(R.styleable.MaxWidthFrameLayout_android_maxWidth, Integer.MAX_VALUE);
		ta.recycle();
	}

	public int getMaxWidth(){
		return maxWidth;
	}

	public void setMaxWidth(int maxWidth){
		this.maxWidth=maxWidth;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(MeasureSpec.getSize(widthMeasureSpec)>maxWidth){
			widthMeasureSpec=maxWidth | MeasureSpec.getMode(widthMeasureSpec);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
