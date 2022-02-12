package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;

public class ProgressBarButton extends Button{
	private boolean textVisible=true;

	public ProgressBarButton(Context context){
		super(context);
	}

	public ProgressBarButton(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public ProgressBarButton(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	public void setTextVisible(boolean textVisible){
		this.textVisible=textVisible;
		invalidate();
	}

	public boolean isTextVisible(){
		return textVisible;
	}

	@Override
	protected void onDraw(Canvas canvas){
		if(textVisible){
			super.onDraw(canvas);
		}
	}
}
