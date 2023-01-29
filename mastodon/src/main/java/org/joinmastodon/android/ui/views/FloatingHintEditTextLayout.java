package org.joinmastodon.android.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class FloatingHintEditTextLayout extends FrameLayout{
	private EditText edit;
	private TextView label;
	private int labelTextSize;
	private int offsetY;
	private boolean hintVisible;
	private Animator currentAnim;
	private float animProgress;
	private RectF tmpRect=new RectF();
	private ColorStateList labelColors, origHintColors;
	private boolean errorState;
	private TextView errorView;

	public FloatingHintEditTextLayout(Context context){
		this(context, null);
	}

	public FloatingHintEditTextLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public FloatingHintEditTextLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		if(isInEditMode())
			V.setApplicationContext(context);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.FloatingHintEditTextLayout);
		labelTextSize=ta.getDimensionPixelSize(R.styleable.FloatingHintEditTextLayout_android_labelTextSize, V.dp(12));
		offsetY=ta.getDimensionPixelOffset(R.styleable.FloatingHintEditTextLayout_editTextOffsetY, 0);
		labelColors=ta.getColorStateList(R.styleable.FloatingHintEditTextLayout_labelTextColor);
		ta.recycle();
		setAddStatesFromChildren(true);
	}

	@Override
	protected void onFinishInflate(){
		super.onFinishInflate();
		if(getChildCount()>0 && getChildAt(0) instanceof EditText et){
			edit=et;
		}else{
			throw new IllegalStateException("First child must be an EditText");
		}

		label=new TextView(getContext());
		label.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize);
//		label.setTextColor(labelColors==null ? edit.getHintTextColors() : labelColors);
		origHintColors=edit.getHintTextColors();
		label.setText(edit.getHint());
		label.setSingleLine();
		label.setPivotX(0f);
		label.setPivotY(0f);
		label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
		LayoutParams lp=new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP);
		lp.setMarginStart(edit.getPaddingStart()+((LayoutParams)edit.getLayoutParams()).getMarginStart());
		addView(label, lp);

		hintVisible=edit.getText().length()==0;
		if(hintVisible)
			label.setAlpha(0f);

		edit.addTextChangedListener(new SimpleTextWatcher(this::onTextChanged));

		errorView=new LinkedTextView(getContext());
		errorView.setTextAppearance(R.style.m3_body_small);
		errorView.setTextColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3OnSurfaceVariant));
		errorView.setLinkTextColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3Primary));
		errorView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		errorView.setPadding(V.dp(16), V.dp(4), V.dp(16), 0);
		errorView.setVisibility(View.GONE);
		addView(errorView);
	}

	private void onTextChanged(Editable text){
		if(errorState){
			errorView.setVisibility(View.GONE);
			errorState=false;
			setForeground(getResources().getDrawable(R.drawable.bg_m3_outlined_text_field, getContext().getTheme()));
			refreshDrawableState();
		}
		boolean newHintVisible=text.length()==0;
		if(newHintVisible==hintVisible)
			return;
		if(currentAnim!=null)
			currentAnim.cancel();
		hintVisible=newHintVisible;

		label.setAlpha(1);
		edit.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				edit.getViewTreeObserver().removeOnPreDrawListener(this);

				float scale=edit.getLineHeight()/(float)label.getLineHeight();
				float transY=edit.getHeight()/2f-edit.getLineHeight()/2f+(edit.getTop()-label.getTop())-(label.getHeight()/2f-label.getLineHeight()/2f);

				AnimatorSet anim=new AnimatorSet();
				if(hintVisible){
					anim.playTogether(
							ObjectAnimator.ofFloat(edit, TRANSLATION_Y, 0),
							ObjectAnimator.ofFloat(label, SCALE_X, scale),
							ObjectAnimator.ofFloat(label, SCALE_Y, scale),
							ObjectAnimator.ofFloat(label, TRANSLATION_Y, transY),
							ObjectAnimator.ofFloat(FloatingHintEditTextLayout.this, "animProgress", 0f)
					);
					edit.setHintTextColor(0);
				}else{
					label.setScaleX(scale);
					label.setScaleY(scale);
					label.setTranslationY(transY);
					anim.playTogether(
							ObjectAnimator.ofFloat(edit, TRANSLATION_Y, offsetY),
							ObjectAnimator.ofFloat(label, SCALE_X, 1f),
							ObjectAnimator.ofFloat(label, SCALE_Y, 1f),
							ObjectAnimator.ofFloat(label, TRANSLATION_Y, 0f),
							ObjectAnimator.ofFloat(FloatingHintEditTextLayout.this, "animProgress", 1f)
					);
				}
				anim.setDuration(150);
				anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
				anim.start();
				anim.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						currentAnim=null;
						if(hintVisible){
							label.setAlpha(0);
							edit.setHintTextColor(origHintColors);
						}
					}
				});
				currentAnim=anim;
				return true;
			}
		});
	}

	@Keep
	public void setAnimProgress(float progress){
		animProgress=progress;
		invalidate();
	}

	@Keep
	public float getAnimProgress(){
		return animProgress;
	}

	@Override
	public void onDrawForeground(Canvas canvas){
		if(getForeground()!=null && animProgress>0){
			canvas.save();
			float width=(label.getWidth()+V.dp(8))*animProgress;
			float centerX=label.getLeft()+label.getWidth()/2f;
			tmpRect.set(centerX-width/2f, label.getTop(), centerX+width/2f, label.getBottom());
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
				canvas.clipOutRect(tmpRect);
			else
				canvas.clipRect(tmpRect, Region.Op.DIFFERENCE);
			super.onDrawForeground(canvas);
			canvas.restore();
		}else{
			super.onDrawForeground(canvas);
		}
	}

	@Override
	public void setForeground(Drawable foreground){
		super.setForeground(new PaddedForegroundDrawable(foreground));
	}

	@Override
	public Drawable getForeground(){
		if(super.getForeground() instanceof PaddedForegroundDrawable pfd){
			return pfd.wrapped;
		}
		return null;
	}

	@Override
	protected void drawableStateChanged(){
		super.drawableStateChanged();
		if(label==null || errorState)
			return;
		ColorStateList color=labelColors==null ? origHintColors : labelColors;
		label.setTextColor(color.getColorForState(getDrawableState(), 0xff00ff00));
	}

	public void setErrorState(CharSequence error){
		if(errorState)
			return;
		errorState=true;
		setForeground(getResources().getDrawable(R.drawable.bg_m3_outlined_text_field_error, getContext().getTheme()));
		label.setTextColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3Error));
		errorView.setVisibility(VISIBLE);
		errorView.setText(error);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(errorView.getVisibility()!=GONE){
			int width=MeasureSpec.getSize(widthMeasureSpec)-getPaddingLeft()-getPaddingRight();
			LayoutParams editLP=(LayoutParams) edit.getLayoutParams();
			width-=editLP.leftMargin+editLP.rightMargin;
			errorView.measure(width | MeasureSpec.EXACTLY, MeasureSpec.UNSPECIFIED);
			LayoutParams lp=(LayoutParams) errorView.getLayoutParams();
			lp.width=width;
			lp.height=errorView.getMeasuredHeight();
			lp.gravity=Gravity.LEFT | Gravity.BOTTOM;
			lp.leftMargin=editLP.leftMargin;
			editLP.bottomMargin=errorView.getMeasuredHeight();
		}else{
			LayoutParams editLP=(LayoutParams) edit.getLayoutParams();
			editLP.bottomMargin=0;
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private class PaddedForegroundDrawable extends Drawable{
		private final Drawable wrapped;

		private PaddedForegroundDrawable(Drawable wrapped){
			this.wrapped=wrapped;
			wrapped.setCallback(new Callback(){
				@Override
				public void invalidateDrawable(@NonNull Drawable who){
					invalidateSelf();
				}

				@Override
				public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when){
					scheduleSelf(what, when);
				}

				@Override
				public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what){
					unscheduleSelf(what);
				}
			});
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			wrapped.draw(canvas);
		}

		@Override
		public void setAlpha(int alpha){
			wrapped.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){
			wrapped.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity(){
			return wrapped.getOpacity();
		}

		@Override
		public boolean setState(@NonNull int[] stateSet){
			return wrapped.setState(stateSet);
		}

		@Override
		public int getLayoutDirection(){
			return wrapped.getLayoutDirection();
		}

		@Override
		public int getAlpha(){
			return wrapped.getAlpha();
		}

		@Nullable
		@Override
		public ColorFilter getColorFilter(){
			return wrapped.getColorFilter();
		}

		@Override
		public boolean isStateful(){
			return wrapped.isStateful();
		}

		@NonNull
		@Override
		public int[] getState(){
			return wrapped.getState();
		}

		@NonNull
		@Override
		public Drawable getCurrent(){
			return wrapped.getCurrent();
		}

		@Override
		public void applyTheme(@NonNull Resources.Theme t){
			wrapped.applyTheme(t);
		}

		@Override
		public boolean canApplyTheme(){
			return wrapped.canApplyTheme();
		}

		@Override
		protected void onBoundsChange(@NonNull Rect bounds){
			super.onBoundsChange(bounds);
			int offset=V.dp(12);
			wrapped.setBounds(edit.getLeft()-offset, edit.getTop()-offset, edit.getRight()+offset, edit.getBottom()+offset);
		}
	}
}
