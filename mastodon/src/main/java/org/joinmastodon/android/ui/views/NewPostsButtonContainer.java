package org.joinmastodon.android.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class NewPostsButtonContainer extends FrameLayout{
	private GestureDetector gestureDetector;
	private Runnable onHideButtonListener;
	private float touchslop;

	public NewPostsButtonContainer(Context context){
		this(context, null);
	}

	public NewPostsButtonContainer(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public NewPostsButtonContainer(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		gestureDetector=new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
			private float totalYOffset;

			@Override
			public boolean onDown(@NonNull MotionEvent e){
				totalYOffset=0;
				getChildAt(0).animate().cancel();
				return false;
			}

			@Override
			public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY){
				totalYOffset+=distanceY;
				getChildAt(0).setTranslationY(-Math.max(0, totalYOffset));
				return totalYOffset>0;
			}
		});
		gestureDetector.setIsLongpressEnabled(false);
		touchslop=ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		getParent().requestDisallowInterceptTouchEvent(true);
		if(gestureDetector.onTouchEvent(ev))
			return true;
		return super.onInterceptTouchEvent(ev);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		boolean r=gestureDetector.onTouchEvent(ev);
		if(ev.getAction()==MotionEvent.ACTION_UP){
			if(!r){
				if(getChildAt(0).getTranslationY()<-touchslop){
					onHideButtonListener.run();
				}else{
					animateBack();
				}
			}
		}else if(ev.getAction()==MotionEvent.ACTION_CANCEL){
			animateBack();
		}
		return r;
	}

	private void animateBack(){
		getChildAt(0).animate().translationY(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
	}

	public void setOnHideButtonListener(Runnable onHideButtonListener){
		this.onHideButtonListener=onHideButtonListener;
	}
}
