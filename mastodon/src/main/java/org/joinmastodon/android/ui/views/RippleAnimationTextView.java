package org.joinmastodon.android.ui.views;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import me.grishka.appkit.utils.CustomViewHelper;

public class RippleAnimationTextView extends TextView implements CustomViewHelper{
	private final Paint animationPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private CharacterAnimationState[] charStates;
	private final ArgbEvaluator colorEvaluator=new ArgbEvaluator();
	private int runningAnimCount=0;
	private Runnable[] delayedAnimations1, delayedAnimations2;

	public RippleAnimationTextView(Context context){
		this(context, null);
	}

	public RippleAnimationTextView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public RippleAnimationTextView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter){
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		if(charStates!=null){
			for(CharacterAnimationState state:charStates){
				state.colorAnimation.cancel();
				state.shadowAnimation.cancel();
				state.scaleAnimation.cancel();
			}
			for(Runnable r:delayedAnimations1){
				if(r!=null)
					removeCallbacks(r);
			}
			for(Runnable r:delayedAnimations2){
				if(r!=null)
					removeCallbacks(r);
			}
		}
		charStates=new CharacterAnimationState[lengthAfter];
		delayedAnimations1=new Runnable[lengthAfter];
		delayedAnimations2=new Runnable[lengthAfter];
	}

	@Override
	protected void onDraw(Canvas canvas){
		if(runningAnimCount==0 && !areThereDelayedAnimations()){
			super.onDraw(canvas);
			return;
		}
		Layout layout=getLayout();
		animationPaint.set(getPaint());
		CharSequence text=layout.getText();
		for(int i=0;i<layout.getLineCount();i++){
			int baseline=layout.getLineBaseline(i);
			for(int offset=layout.getLineStart(i); offset<layout.getLineEnd(i); offset++){
				float x=layout.getPrimaryHorizontal(offset);
				CharacterAnimationState state=charStates[offset];
				if(state==null || state.scaleAnimation==null){
					animationPaint.setColor(getCurrentTextColor());
					animationPaint.clearShadowLayer();
					canvas.drawText(text, offset, offset+1, x, baseline, animationPaint);
				}else{
					animationPaint.setColor((int)colorEvaluator.evaluate(Math.max(0, Math.min(1, state.color.getValue())), getCurrentTextColor(), getLinkTextColors().getDefaultColor()));
					float scale=state.scale.getValue();
					int shadowAlpha=Math.round(255*Math.max(0, Math.min(1, state.shadowAlpha.getValue())));
					animationPaint.setShadowLayer(dp(4), 0, dp(3), (getPaint().linkColor & 0xFFFFFF) | (shadowAlpha << 24));
					canvas.save();
					canvas.scale(scale, scale, x, baseline);
					canvas.drawText(text, offset, offset+1, x, baseline, animationPaint);
					canvas.restore();
				}
			}
		}
		invalidate();
	}

	public void animate(int startIndex, int endIndex){
		for(int i=startIndex;i<endIndex;i++){
			CharacterAnimationState _state=charStates[i];
			if(_state==null){
				_state=charStates[i]=new CharacterAnimationState();
			}
			CharacterAnimationState state=_state;
			int finalI=i;
			postOnAnimationDelayed(()->{
				if(!state.colorAnimation.isRunning())
					runningAnimCount++;
				state.colorAnimation.animateToFinalPosition(1f);
				if(!state.shadowAnimation.isRunning())
					runningAnimCount++;
				state.shadowAnimation.animateToFinalPosition(0.3f);
				if(!state.scaleAnimation.isRunning())
					runningAnimCount++;
				state.scaleAnimation.animateToFinalPosition(1.2f);
				invalidate();

				if(delayedAnimations1[finalI]!=null)
					removeCallbacks(delayedAnimations1[finalI]);
				if(delayedAnimations2[finalI]!=null)
					removeCallbacks(delayedAnimations2[finalI]);
				Runnable delay1=()->{
					if(!state.colorAnimation.isRunning())
						runningAnimCount++;
					state.colorAnimation.animateToFinalPosition(0f);
					if(!state.shadowAnimation.isRunning())
						runningAnimCount++;
					state.shadowAnimation.animateToFinalPosition(0f);
					invalidate();
					delayedAnimations1[finalI]=null;
				};
				Runnable delay2=()->{
					if(!state.scaleAnimation.isRunning())
						runningAnimCount++;
					state.scaleAnimation.animateToFinalPosition(1f);
					delayedAnimations2[finalI]=null;
				};
				delayedAnimations1[finalI]=delay1;
				delayedAnimations2[finalI]=delay2;
				postOnAnimationDelayed(delay1, 2000);
				postOnAnimationDelayed(delay2, 100);
			}, 20L*(i-startIndex));
		}
	}

	private boolean areThereDelayedAnimations(){
		for(Runnable r:delayedAnimations1){
			if(r!=null)
				return true;
		}
		for(Runnable r:delayedAnimations2){
			if(r!=null)
				return true;
		}
		return false;
	}

	private class CharacterAnimationState extends FloatValueHolder{
		private final SpringAnimation scaleAnimation, colorAnimation, shadowAnimation;
		private final FloatValueHolder scale=new FloatValueHolder(1), color=new FloatValueHolder(), shadowAlpha=new FloatValueHolder();

		public CharacterAnimationState(){
			scaleAnimation=new SpringAnimation(scale);
			colorAnimation=new SpringAnimation(color);
			shadowAnimation=new SpringAnimation(shadowAlpha);
			setupSpring(scaleAnimation);
			setupSpring(colorAnimation);
			setupSpring(shadowAnimation);
		}

		private void setupSpring(SpringAnimation anim){
			anim.setMinimumVisibleChange(0.01f);
			anim.setSpring(new SpringForce().setStiffness(500f).setDampingRatio(0.175f));
			anim.addEndListener((animation, canceled, value, velocity)->runningAnimCount--);
		}
	}
}
