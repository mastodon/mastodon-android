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
import android.view.Gravity;
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

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.Keep;
import androidx.annotation.StringRes;
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

	private Snackbar(Context context, String text, String action, Runnable onActionClick, int bottomOffset){
		this.context=context;
		this.bottomOffset=bottomOffset;
		hasAction=onActionClick!=null;

		windowView=new FrameLayout(context);
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
		if(current!=null)
			current.dismiss();
		current=this;
		WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
		lp.width=ViewGroup.LayoutParams.MATCH_PARENT;
		lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
		lp.gravity=Gravity.BOTTOM;
		lp.y=bottomOffset;
		WindowManager wm=context.getSystemService(WindowManager.class);
		wm.addView(windowView, lp);
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
		windowView.postDelayed(dismissRunnable, 4000);
	}

	public void dismiss(){
		current=null;
		if(currentAnim!=null){
			currentAnim.cancel();
		}
		windowView.removeCallbacks(dismissRunnable);
		ObjectAnimator anim=ObjectAnimator.ofFloat(contentView, View.ALPHA, 0);
		anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
		anim.setDuration(200);
		anim.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				WindowManager wm=context.getSystemService(WindowManager.class);
				wm.removeView(windowView);
			}
		});
		anim.start();
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
			return new Snackbar(context, text, action, onActionClick, bottomOffset);
		}

		public void show(){
			create().show();
		}
	}
}
