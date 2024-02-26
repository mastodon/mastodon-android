package org.joinmastodon.android.ui.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class HorizontalScrollingTouchListener implements View.OnTouchListener{
	private float downX, touchslop;
	private boolean didDisallow;

	public HorizontalScrollingTouchListener(Context context){
		touchslop=ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onTouch(View v, MotionEvent ev){
		if(ev.getAction()==MotionEvent.ACTION_DOWN){
			if(v.canScrollHorizontally(-1) || v.canScrollHorizontally(1)){
				v.getParent().requestDisallowInterceptTouchEvent(true);
				didDisallow=true;
			}else{
				didDisallow=false;
			}
			downX=ev.getX();
		}else if(didDisallow && ev.getAction()==MotionEvent.ACTION_MOVE){
			if(Math.abs(downX-ev.getX())>=touchslop){
				if(!v.canScrollHorizontally((int) (downX-ev.getX()))){
					didDisallow=false;
					v.getParent().requestDisallowInterceptTouchEvent(false);
				}
			}
		}
		return false;
	}
}
