package org.joinmastodon.android.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;

import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class FloatingHintEditTextLayout extends FrameLayout{
	private EditText edit;
	private TextView label;
	private int labelTextSize;
	private int offsetY;
	private boolean hintVisible;
	private Animator currentAnim;

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
		ta.recycle();
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
		label.setTextColor(edit.getHintTextColors());
		label.setText(edit.getHint());
		label.setSingleLine();
		label.setPivotX(0f);
		label.setPivotY(0f);
		LayoutParams lp=new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP);
		lp.setMarginStart(edit.getPaddingStart());
		addView(label, lp);

		hintVisible=edit.getText().length()==0;
		if(hintVisible)
			label.setAlpha(0f);

		edit.addTextChangedListener(new SimpleTextWatcher(this::onTextChanged));
	}

	private void onTextChanged(Editable text){
		boolean newHintVisible=text.length()==0;
		if(newHintVisible==hintVisible)
			return;
		if(currentAnim!=null)
			currentAnim.cancel();
		hintVisible=newHintVisible;

		label.setAlpha(1);
		float scale=edit.getLineHeight()/(float)label.getLineHeight();
		float transY=edit.getHeight()/2f-edit.getLineHeight()/2f+(edit.getTop()-label.getTop())-(label.getHeight()/2f-label.getLineHeight()/2f);

		AnimatorSet anim=new AnimatorSet();
		if(hintVisible){
			anim.playTogether(
					ObjectAnimator.ofFloat(edit, TRANSLATION_Y, 0),
					ObjectAnimator.ofFloat(label, SCALE_X, scale),
					ObjectAnimator.ofFloat(label, SCALE_Y, scale),
					ObjectAnimator.ofFloat(label, TRANSLATION_Y, transY)
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
					ObjectAnimator.ofFloat(label, TRANSLATION_Y, 0f)
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
					edit.setHintTextColor(label.getTextColors());
				}
			}
		});
		currentAnim=anim;
	}
}
