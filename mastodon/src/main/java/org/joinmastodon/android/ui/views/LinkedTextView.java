package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import org.joinmastodon.android.ui.text.ClickableLinksDelegate;

public class LinkedTextView extends TextView {

	private ClickableLinksDelegate delegate=new ClickableLinksDelegate(this);
	private boolean needInvalidate;
	
	public LinkedTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public LinkedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public LinkedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	public boolean onTouchEvent(MotionEvent ev){
		if(delegate.onTouch(ev)) return true;
        return super.onTouchEvent(ev);
	}
	
	public void onDraw(Canvas c){
		super.onDraw(c);
		delegate.onDraw(c);
		if(needInvalidate)
			invalidate();
	}

	// a hack to support animated emoji on <9.0
	public void setInvalidateOnEveryFrame(boolean invalidate){
		needInvalidate=invalidate;
		if(invalidate)
			invalidate();
	}

}
