package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import org.joinmastodon.android.R;

public class ProgressBarButton extends Button{
	private boolean textVisible=true;
	private ProgressBar progressBar;
	private int progressBarID;

	public ProgressBarButton(Context context){
		this(context, null);
	}

	public ProgressBarButton(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public ProgressBarButton(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.ProgressBarButton);
		progressBarID=ta.getResourceId(R.styleable.ProgressBarButton_progressBar, 0);
		ta.recycle();
	}

	@Override
	protected void onAttachedToWindow(){
		super.onAttachedToWindow();
		if(progressBarID!=0){
			progressBar=((ViewGroup)getParent()).findViewById(progressBarID);
		}
	}

	public void setTextVisible(boolean textVisible){
		this.textVisible=textVisible;
		invalidate();
	}

	public boolean isTextVisible(){
		return textVisible;
	}

	public void setProgressBarVisible(boolean visible){
		if(progressBar==null)
			throw new IllegalStateException("progressBar is not set");
		if(visible){
			setTextVisible(false);
			progressBar.setIndeterminateTintList(getTextColors());
			progressBar.setVisibility(View.VISIBLE);
		}else{
			setTextVisible(true);
			progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onDraw(Canvas canvas){
		if(textVisible){
			super.onDraw(canvas);
		}
	}
}
