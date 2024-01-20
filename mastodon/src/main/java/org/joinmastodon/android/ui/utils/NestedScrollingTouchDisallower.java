package org.joinmastodon.android.ui.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class NestedScrollingTouchDisallower implements View.OnTouchListener{
	private float downY, touchslop;
	private boolean didDisallow;

	public NestedScrollingTouchDisallower(View v){
		touchslop=ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent ev){
		if(ev.getAction()==MotionEvent.ACTION_DOWN){
			if(v.canScrollVertically(-1) || v.canScrollVertically(1)){
				v.getParent().requestDisallowInterceptTouchEvent(true);
				didDisallow=true;
			}else{
				didDisallow=false;
			}
			downY=ev.getY();
		}else if(didDisallow && ev.getAction()==MotionEvent.ACTION_MOVE){
			if(Math.abs(downY-ev.getY())>=touchslop){
				if(!v.canScrollVertically((int)(downY-ev.getY()))){
					didDisallow=false;
					v.getParent().requestDisallowInterceptTouchEvent(false);
				}
			}
		}
		return false;
	}
}
