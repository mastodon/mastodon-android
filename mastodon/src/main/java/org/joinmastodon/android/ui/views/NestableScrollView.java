package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

public class NestableScrollView extends ScrollView{
	private float downY, touchslop;
	private boolean didDisallow;

	public NestableScrollView(Context context){
		super(context);
		init();
	}

	public NestableScrollView(Context context, AttributeSet attrs){
		super(context, attrs);
		init();
	}

	public NestableScrollView(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
		init();
	}

	public NestableScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init(){
		touchslop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev){
		Log.d("111", "onTouchEvent: "+ev);
		if(ev.getAction()==MotionEvent.ACTION_DOWN){
			if(canScrollVertically(-1) || canScrollVertically(1)){
				getParent().requestDisallowInterceptTouchEvent(true);
				didDisallow=true;
			}else{
				didDisallow=false;
			}
			downY=ev.getY();
		}else if(didDisallow && ev.getAction()==MotionEvent.ACTION_MOVE){
			if(Math.abs(downY-ev.getY())>=touchslop){
				if(!canScrollVertically((int)(downY-ev.getY()))){
					didDisallow=false;
					getParent().requestDisallowInterceptTouchEvent(false);
				}
			}
		}
		return super.onTouchEvent(ev);
	}
}
