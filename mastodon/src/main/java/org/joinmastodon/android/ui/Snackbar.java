package org.joinmastodon.android.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class Snackbar{
	private static Snackbar current;

	private final Context context;
	private int bottomOffset;
	private FrameLayout windowView;
	private LinearLayout contentView;
	private boolean hasAction;
	private AnimatableOutlineProvider outlineProvider;
	private Animator currentAnim;
	private Runnable dismissRunnable=this::dismiss;
	private ViewGroup containerView;
	private WindowManager.LayoutParams windowLayoutParams;
	private boolean persistent;
	private float swipeDistance;
	private boolean dismissedByGesture;
	private Runnable dismissListener;
	private boolean ignoreTouchEvents;
	private GestureDetector gestureDetector;
	private GestureDetector.SimpleOnGestureListener gestureListener=new GestureDetector.SimpleOnGestureListener(){
		@Override
		public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY){
			if(Math.abs(velocityX)>Math.abs(velocityY)){
				dismissByGesture(velocityX, velocityX>0);
			}
			return true;
		}

		@Override
		public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY){
			swipeDistance-=distanceX;
			contentView.setTranslationX(swipeDistance);
			contentView.setAlpha(1f-Math.abs(swipeDistance)/contentView.getWidth());
			return true;
		}
	};

	private Snackbar(Context context, String text, String action, Runnable onActionClick, int bottomOffset, boolean persistent){
		this.context=context;
		this.bottomOffset=bottomOffset;
		hasAction=onActionClick!=null;
		this.persistent=persistent;

		gestureDetector=new GestureDetector(context, gestureListener);

		windowView=new WindowFrameLayout(context);
		windowView.setClipToPadding(false);
		contentView=new LinearLayout(context);
		contentView.setOrientation(LinearLayout.HORIZONTAL);
		contentView.setBaselineAligned(false);
		contentView.setBackgroundColor(UiUtils.getThemeColor(context, R.attr.colorM3SurfaceInverse));
		contentView.setOutlineProvider(outlineProvider=new AnimatableOutlineProvider(contentView));
		contentView.setClipToOutline(true);
		contentView.setElevation(V.dp(6));
		contentView.setPaddingRelative(V.dp(16), 0, V.dp(8), 0);
		FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.leftMargin=lp.topMargin=lp.rightMargin=lp.bottomMargin=V.dp(16);
		windowView.addView(contentView, lp);

		TextView textView=new TextView(context);
		textView.setTextAppearance(R.style.m3_body_medium);
		textView.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3OnSurfaceInverse));
		textView.setMaxLines(2);
		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setText(text);
		textView.setMinHeight(V.dp(48));
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
		textView.setPadding(0, V.dp(14), 0, V.dp(14));
		contentView.addView(textView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		if(action!=null){
			Button button=new Button(context);
			int primaryInverse=UiUtils.getThemeColor(context, R.attr.colorM3PrimaryInverse);
			button.setTextColor(primaryInverse);
			button.setBackgroundResource(R.drawable.bg_rect_4dp_ripple);
			button.setBackgroundTintList(ColorStateList.valueOf(primaryInverse));
			button.setText(action);
			button.setPadding(V.dp(8), 0, V.dp(8), 0);
			button.setOnClickListener(v->onActionClick.run());
			LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(40));
			blp.leftMargin=blp.topMargin=blp.rightMargin=blp.bottomMargin=V.dp(4);
			contentView.addView(button, blp);
		}
	}

	public void show(){
		Snackbar currentSnackbar=getCurrent();
		if(currentSnackbar!=null)
			currentSnackbar.dismiss();
		setCurrent(this);
		WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.LAST_APPLICATION_WINDOW, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
		lp.width=ViewGroup.LayoutParams.MATCH_PARENT;
		lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
		lp.gravity=Gravity.BOTTOM;
		lp.y=bottomOffset;
		windowLayoutParams=lp;
		WindowManager wm=context.getSystemService(WindowManager.class);
		wm.addView(windowView, lp);
		playShowAnimation();
	}

	public void showInView(FrameLayout container){
		containerView=container;
		Snackbar currentSnackbar=getCurrent();
		if(currentSnackbar!=null)
			currentSnackbar.dismiss();
		setCurrent(this);
		FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.bottomMargin=bottomOffset;
		lp.gravity=Gravity.BOTTOM;
		container.addView(windowView, lp);
		playShowAnimation();
	}

	private void playShowAnimation(){
		windowView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				windowView.getViewTreeObserver().removeOnPreDrawListener(this);

				AnimatorSet set=new AnimatorSet();
				set.playTogether(
						ObjectAnimator.ofFloat(outlineProvider, "fraction", 0, 1),
						ObjectAnimator.ofFloat(contentView, View.ALPHA, 0, 1)
				);
				set.setInterpolator(AnimationUtils.loadInterpolator(context, R.interpolator.m3_sys_motion_easing_standard_decelerate));
				set.setDuration(350);
				set.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						currentAnim=null;
					}
				});
				currentAnim=set;
				set.start();

				return true;
			}
		});
		if(!persistent)
			windowView.postDelayed(dismissRunnable, 4000);
	}

	private void disableTouchEvents(){
		if(containerView==null){
			windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
			context.getSystemService(WindowManager.class).updateViewLayout(windowView, windowLayoutParams);
		}else{
			ignoreTouchEvents=true;
		}
	}

	public void setDismissListener(Runnable dismissListener){
		this.dismissListener=dismissListener;
	}

	public void dismiss(){
		if(getCurrent()==this)
			setCurrent(null);
		if(currentAnim!=null){
			currentAnim.cancel();
		}
		disableTouchEvents();
		if(dismissListener!=null){
			dismissListener.run();
			dismissListener=null;
		}
		windowView.removeCallbacks(dismissRunnable);
		ObjectAnimator anim=ObjectAnimator.ofFloat(contentView, View.ALPHA, 0);
		anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
		anim.setDuration(200);
		anim.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				dismissImmediately();
			}
		});
		anim.start();
	}

	public void dismissImmediately(){
		if(getCurrent()==this)
			setCurrent(null);
		if(dismissListener!=null)
			dismissListener.run();
		if(containerView!=null){
			containerView.removeView(windowView);
		}else if(windowView.isAttachedToWindow()){
			WindowManager wm=context.getSystemService(WindowManager.class);
			wm.removeView(windowView);
		}
	}

	private void dismissByGesture(float velocityX, boolean toRight){
		if(getCurrent()==this)
			setCurrent(null);
		dismissedByGesture=true;
		disableTouchEvents();
		if(dismissListener!=null){
			dismissListener.run();
			dismissListener=null;
		}
		SpringAnimation anim=new SpringAnimation(contentView, DynamicAnimation.TRANSLATION_X, toRight ? contentView.getWidth()*2 : contentView.getWidth()*(-1.5f));
		anim.setStartVelocity(velocityX);
		anim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
		anim.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
		anim.addEndListener((animation, canceled, value, velocity)->dismissImmediately());
		anim.start();
	}

	private Snackbar getCurrent(){
		if(containerView!=null)
			return (Snackbar) containerView.getTag(R.id.current_snackbar);
		return current;
	}

	private void setCurrent(Snackbar sb){
		if(containerView!=null)
			containerView.setTag(R.id.current_snackbar, sb);
		else
			current=sb;
	}

	private static class AnimatableOutlineProvider extends ViewOutlineProvider{
		private float fraction=1f;
		private final View view;

		private AnimatableOutlineProvider(View view){
			this.view=view;
		}

		@Override
		public void getOutline(View view, Outline outline){
			outline.setRoundRect(0, Math.round(view.getHeight()*(1f-fraction)), view.getWidth(), view.getHeight(), V.dp(4));
		}

		@Keep
		public float getFraction(){
			return fraction;
		}

		@Keep
		public void setFraction(float fraction){
			this.fraction=fraction;
			view.invalidateOutline();
		}
	}

	public static class Builder{
		private final Context context;
		private String text;
		private String action;
		private Runnable onActionClick;
		private int bottomOffset;
		private boolean persistent;

		public Builder(Context context){
			this.context=context;
		}

		public Builder setText(String text){
			this.text=text;
			return this;
		}

		public Builder setText(@StringRes int res){
			text=context.getString(res);
			return this;
		}

		public Builder setAction(String action, Runnable onActionClick){
			this.action=action;
			this.onActionClick=onActionClick;
			return this;
		}

		public Builder setAction(@StringRes int action, Runnable onActionClick){
			this.action=context.getString(action);
			this.onActionClick=onActionClick;
			return this;
		}

		public Builder setBottomOffset(int offset){
			bottomOffset=offset;
			return this;
		}

		public Snackbar create(){
			return new Snackbar(context, text, action, onActionClick, bottomOffset, persistent);
		}

		public void show(){
			create().show();
		}

		public Builder setPersistent(){
			persistent=true;
			return this;
		}
	}

	private class WindowFrameLayout extends FrameLayout{
		public WindowFrameLayout(@NonNull Context context){
			super(context);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev){
			return ignoreTouchEvents || super.dispatchTouchEvent(ev);
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev){
			return gestureDetector.onTouchEvent(ev) || super.onInterceptTouchEvent(ev);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev){
			boolean detectorResult=gestureDetector.onTouchEvent(ev);
			int action=ev.getAction();
			if(action==MotionEvent.ACTION_DOWN){
				if(containerView!=null)
					containerView.requestDisallowInterceptTouchEvent(true);
				if(!persistent)
					windowView.removeCallbacks(dismissRunnable);
				return true;
			}else if(!dismissedByGesture && (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL)){
				float transX=contentView.getTranslationX();
				if(transX!=0){
					if(Math.abs(transX)>contentView.getWidth()/3f){
						dismissedByGesture=true;
						dismissByGesture(0, transX>0);
						return true;
					}else{
						AnimatorSet set=new AnimatorSet();
						set.playTogether(
								ObjectAnimator.ofFloat(contentView, View.TRANSLATION_X, 0),
								ObjectAnimator.ofFloat(contentView, View.ALPHA, 1)
						);
						set.setInterpolator(CubicBezierInterpolator.DEFAULT);
						set.setDuration(200);
						currentAnim=set;
						set.addListener(new AnimatorListenerAdapter(){
							@Override
							public void onAnimationEnd(Animator animation){
								currentAnim=null;
							}
						});
						set.start();
					}
				}
				if(!persistent)
					windowView.postDelayed(dismissRunnable, 4000);
				swipeDistance=0;
			}
			return detectorResult || super.onTouchEvent(ev);
		}
	}
}
