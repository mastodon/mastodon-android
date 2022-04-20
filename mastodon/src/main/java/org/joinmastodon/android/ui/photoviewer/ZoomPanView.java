package org.joinmastodon.android.ui.photoviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import java.util.ArrayList;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import me.grishka.appkit.utils.V;

public class ZoomPanView extends FrameLayout implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{
	private View child;
	private Matrix matrix=new Matrix();
	private float[] matrixValues=new float[9];
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	private OverScroller scroller;
	private boolean scaling, scrolling, swipingToDismiss, wasScaling, animatingTransform, animatingTransition, dismissAfterTransition, animatingCanceledDismiss;
	private boolean wasAnimatingTransition; // to drop any sequences of touch events that start during animation but continue after it

	// these keep track of view translation/scrolling
	private float transX, transY;
	// translation/scrolling limits, updated whenever scale changes
	private float minTransX, minTransY, maxTransX, maxTransY;
	// total scroll offsets since the last ACTION_DOWN event, to detect scrolling axis
	private float totalScrollX, totalScrollY;
	// scale factor limits
	private float minScale, maxScale;
	// coordinates of the last scale gesture, to undo extra if it goes above maxScale
	private float lastScaleCenterX, lastScaleCenterY;
	private boolean canScrollLeft, canScrollRight;
	private ArrayList<SpringAnimation> runningTransformAnimations=new ArrayList<>(), runningTransitionAnimations=new ArrayList<>();

	private RectF tmpRect=new RectF(), tmpRect2=new RectF();
	// the initial/final crop rect for open/close transitions, in child coordinates
	private RectF transitionCropRect=new RectF();
	private float cropAnimationValue, rawCropAndFadeValue;
	private float lastFlingVelocityY;
	private float backgroundAlphaForTransition=1f;
	private boolean forceUpdateLayout;

	private static final String TAG="ZoomPanView";

	private Runnable scrollerUpdater=this::doScrollerAnimation;
	private Listener listener;
	private static final FloatPropertyCompat<ZoomPanView> CROP_AND_FADE=new FloatPropertyCompat<>("cropAndFade"){
		@Override
		public float getValue(ZoomPanView object){
			return object.rawCropAndFadeValue;
		}

		@Override
		public void setValue(ZoomPanView object, float value){
			object.rawCropAndFadeValue=value;
			if(value>0.1f)
				object.child.setAlpha(Math.min((value-0.1f)/0.4f, 1f));
			else
				object.child.setAlpha(0f);

			if(value>0.3f)
				object.setCropAnimationValue(Math.min(1f, (value-0.3f)/0.7f));
			else
				object.setCropAnimationValue(0f);

			if(value>0.5f)
				object.listener.onSetBackgroundAlpha(Math.min(1f, (value-0.5f)/0.5f*object.backgroundAlphaForTransition));
			else
				object.listener.onSetBackgroundAlpha(0f);

			object.invalidate();
		}
	};

	public ZoomPanView(Context context){
		this(context, null);
	}

	public ZoomPanView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public ZoomPanView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		gestureDetector=new GestureDetector(context, this);
		gestureDetector.setIsLongpressEnabled(false);
		gestureDetector.setOnDoubleTapListener(this);
		scaleDetector=new ScaleGestureDetector(context, this);
		scroller=new OverScroller(context);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom){
		super.onLayout(changed, left, top, right, bottom);
		if(!changed && child!=null && !forceUpdateLayout)
			return;
		child=getChildAt(0);
		if(child==null)
			return;

		int width=right-left;
		int height=bottom-top;
		float scale=Math.min(width/(float)child.getWidth(), height/(float)child.getHeight());
		minScale=scale;
		maxScale=Math.max(3f, height/(float)child.getHeight());
		matrix.setScale(scale, scale);
		if(!animatingTransition)
			updateViewTransform(false);
		updateLimits(scale);
		transX=transY=0;
		if(forceUpdateLayout)
			forceUpdateLayout=false;
	}

	public void updateLayout(){
		forceUpdateLayout=true;
		requestLayout();
	}

	private float interpolate(float a, float b, float k){
		return a+(b-a)*k;
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime){
		if(!canvas.isHardwareAccelerated())
			return false;
		if(child==this.child && animatingTransition){
			tmpRect.set(0, 0, child.getWidth(), child.getHeight());
			child.getMatrix().mapRect(tmpRect);
			tmpRect.offset(child.getLeft(), child.getTop());
			tmpRect2.set(transitionCropRect);
			child.getMatrix().mapRect(tmpRect2);
			tmpRect2.offset(child.getLeft(), child.getTop());
			canvas.save();
			canvas.clipRect(interpolate(tmpRect2.left, tmpRect.left, cropAnimationValue),
					interpolate(tmpRect2.top, tmpRect.top, cropAnimationValue),
					interpolate(tmpRect2.right, tmpRect.right, cropAnimationValue),
					interpolate(tmpRect2.bottom, tmpRect.bottom, cropAnimationValue));
			boolean res=super.drawChild(canvas, child, drawingTime);
			canvas.restore();
			return res;
		}
		return super.drawChild(canvas, child, drawingTime);
	}

	public void setListener(Listener listener){
		this.listener=listener;
	}

	private void setCropAnimationValue(float val){
		cropAnimationValue=val;
	}

	private float prepareTransitionCropRect(Rect rect){
		float initialScale;
		float scaleW=rect.width()/(float)child.getWidth();
		float scaleH=rect.height()/(float)child.getHeight();
		if(scaleW>scaleH){
			initialScale=scaleW;
			float scaledHeight=rect.height()/scaleW;
			transitionCropRect.left=0;
			transitionCropRect.right=child.getWidth();
			transitionCropRect.top=child.getHeight()/2f-scaledHeight/2f;
			transitionCropRect.bottom=transitionCropRect.top+scaledHeight;
		}else{
			initialScale=scaleH;
			float scaledWidth=rect.width()/scaleH;
			transitionCropRect.top=0;
			transitionCropRect.bottom=child.getHeight();
			transitionCropRect.left=child.getWidth()/2f-scaledWidth/2f;
			transitionCropRect.right=transitionCropRect.left+scaledWidth;
		}
		return initialScale;
	}

	public void animateIn(Rect rect, int[] cornerRadius){
		int[] loc={0, 0};
		getLocationOnScreen(loc);
		int centerX=loc[0]+getWidth()/2;
		int centerY=loc[1]+getHeight()/2;
		float initialTransX=rect.centerX()-centerX;
		float initialTransY=rect.centerY()-centerY;
		child.setTranslationX(initialTransX);
		child.setTranslationY(initialTransY);
		float initialScale=prepareTransitionCropRect(rect);
		child.setScaleX(initialScale);
		child.setScaleY(initialScale);
		animatingTransition=true;

		matrix.getValues(matrixValues);

		child.setAlpha(0f);
		setupAndStartTransitionAnim(new SpringAnimation(this, CROP_AND_FADE, 1f).setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.SCALE_X, matrixValues[Matrix.MSCALE_X]));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.SCALE_Y, matrixValues[Matrix.MSCALE_Y]));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_X, matrixValues[Matrix.MTRANS_X]));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_Y, matrixValues[Matrix.MTRANS_Y]));
		postOnAnimation(new Runnable(){
			@Override
			public void run(){
				if(animatingTransition){
					listener.onTransitionAnimationUpdate(child.getTranslationX()-initialTransX, child.getTranslationY()-initialTransY, child.getScaleX()/initialScale);
					postOnAnimation(this);
				}
			}
		});
	}

	public void animateOut(Rect rect, int[] cornerRadius, float velocityY){
		int[] loc={0, 0};
		getLocationOnScreen(loc);
		int centerX=loc[0]+getWidth()/2;
		int centerY=loc[1]+getHeight()/2;
		float initialTransX=rect.centerX()-centerX;
		float initialTransY=rect.centerY()-centerY;
		float initialScale=prepareTransitionCropRect(rect);
		animatingTransition=true;
		dismissAfterTransition=true;
		rawCropAndFadeValue=1f;

		setupAndStartTransitionAnim(new SpringAnimation(this, CROP_AND_FADE, 0f).setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.SCALE_X, initialScale));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.SCALE_Y, initialScale));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_X, initialTransX));
		setupAndStartTransitionAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_Y, initialTransY).setStartVelocity(velocityY));
		postOnAnimation(new Runnable(){
			@Override
			public void run(){
				if(animatingTransition){
					listener.onTransitionAnimationUpdate(child.getTranslationX()-initialTransX, child.getTranslationY()-initialTransY, child.getScaleX()/initialScale);
					postOnAnimation(this);
				}
			}
		});
	}

	private void updateViewTransform(boolean animated){
		matrix.getValues(matrixValues);
		if(animated){
			animatingTransform=true;
			setupAndStartTransformAnim(new SpringAnimation(child, DynamicAnimation.SCALE_X, matrixValues[Matrix.MSCALE_X]));
			setupAndStartTransformAnim(new SpringAnimation(child, DynamicAnimation.SCALE_Y, matrixValues[Matrix.MSCALE_Y]));
			setupAndStartTransformAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_X, matrixValues[Matrix.MTRANS_X]));
			setupAndStartTransformAnim(new SpringAnimation(child, DynamicAnimation.TRANSLATION_Y, matrixValues[Matrix.MTRANS_Y]));
			if(backgroundAlphaForTransition<1f){
				setupAndStartTransformAnim(new SpringAnimation(this, new FloatPropertyCompat<>("backgroundAlpha"){
					@Override
					public float getValue(ZoomPanView object){
						return backgroundAlphaForTransition;
					}

					@Override
					public void setValue(ZoomPanView object, float value){
						backgroundAlphaForTransition=value;
						listener.onSetBackgroundAlpha(value);
					}
				}, 1f).setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA));
			}
		}else{
			if(animatingTransition)
				Log.w(TAG, "updateViewTransform: ", new Throwable().fillInStackTrace());
			child.setScaleX(matrixValues[Matrix.MSCALE_X]);
			child.setScaleY(matrixValues[Matrix.MSCALE_Y]);
			child.setTranslationX(matrixValues[Matrix.MTRANS_X]);
			child.setTranslationY(matrixValues[Matrix.MTRANS_Y]);
		}
	}

	private void updateLimits(float targetScale){
		float scaledWidth=child.getWidth()*targetScale;
		float scaledHeight=child.getHeight()*targetScale;
		if(scaledWidth>getWidth()){
			minTransX=(getWidth()-Math.round(scaledWidth))/2f;
			maxTransX=-minTransX;
		}else{
			minTransX=maxTransX=0f;
		}
		if(scaledHeight>getHeight()){
			minTransY=(getHeight()-Math.round(scaledHeight))/2f;
			maxTransY=-minTransY;
		}else{
			minTransY=maxTransY=0f;
		}
	}

	private void springBack(){
		if(child.getScaleX()<minScale){
			matrix.setScale(minScale, minScale);
			updateViewTransform(true);
			updateLimits(minScale);
			transX=transY=0;
			return;
		}
		boolean needAnimate=false;
		if(child.getScaleX()>maxScale){
			float scaleCorrection=maxScale/child.getScaleX();
			matrix.postScale(scaleCorrection, scaleCorrection, lastScaleCenterX, lastScaleCenterY);
			matrix.getValues(matrixValues);
			transX=matrixValues[Matrix.MTRANS_X];
			transY=matrixValues[Matrix.MTRANS_Y];
			updateLimits(maxScale);
			needAnimate=true;
		}
		needAnimate|=clampMatrixTranslationToLimits();
		if(needAnimate){
			updateViewTransform(true);
		}else if(animatingCanceledDismiss){
			animatingCanceledDismiss=false;
		}
	}

	private boolean clampMatrixTranslationToLimits(){
		boolean needAnimate=false;
		float dtx=0f, dty=0f;
		if(transX>maxTransX){
			dtx=maxTransX-transX;
			transX=maxTransX;
			needAnimate=true;
		}else if(transX<minTransX){
			dtx=minTransX-transX;
			transX=minTransX;
			needAnimate=true;
		}

		if(transY>maxTransY){
			dty=maxTransY-transY;
			transY=maxTransY;
			needAnimate=true;
		}else if(transY<minTransY){
			dty=minTransY-transY;
			transY=minTransY;
			needAnimate=true;
		}
		if(needAnimate)
			matrix.postTranslate(dtx, dty);
		return needAnimate;
	}

	public void setScrollDirections(boolean left, boolean right){
		canScrollLeft=left;
		canScrollRight=right;
	}

	private void onTransformAnimationEnd(DynamicAnimation<?> animation, boolean canceled, float value, float velocity){
		runningTransformAnimations.remove(animation);
		if(runningTransformAnimations.isEmpty()){
			animatingTransform=false;
			if(animatingCanceledDismiss){
				animatingCanceledDismiss=false;
				listener.onSwipeToDismissCanceled();
			}
		}
	}

	private void onTransitionAnimationEnd(DynamicAnimation<?> animation, boolean canceled, float value, float velocity){
		if(runningTransitionAnimations.remove(animation) && runningTransitionAnimations.isEmpty()){
			animatingTransition=false;
			wasAnimatingTransition=true;
			listener.onTransitionAnimationFinished();
			if(dismissAfterTransition)
				listener.onDismissed();
			else
				invalidate();
		}
	}

	private void setupAndStartTransformAnim(SpringAnimation anim){
		anim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
		anim.addEndListener(this::onTransformAnimationEnd).start();
		runningTransformAnimations.add(anim);
	}

	private void setupAndStartTransitionAnim(SpringAnimation anim){
		anim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
		anim.addEndListener(this::onTransitionAnimationEnd).start();
		runningTransitionAnimations.add(anim);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		boolean isUp=ev.getAction()==MotionEvent.ACTION_UP || ev.getAction()==MotionEvent.ACTION_CANCEL;
		if(animatingTransition && ev.getAction()==MotionEvent.ACTION_DOWN){
			ArrayList<SpringAnimation> anims=new ArrayList<>(runningTransitionAnimations);
			for(SpringAnimation anim:anims){
				anim.skipToEnd();
				onTransitionAnimationEnd(anim, true, 0f, 0f);
			}
		}
		scaleDetector.onTouchEvent(ev);
		if(!swipingToDismiss && isUp){
			if(scrolling || wasScaling){
				scrolling=false;
				wasScaling=false;
				springBack();
			}
		}
		if(scaling)
			return true;
		gestureDetector.onTouchEvent(ev);
		if(swipingToDismiss && isUp){
			swipingToDismiss=false;
			scrolling=false;
			if(Math.abs(child.getTranslationY())>getHeight()/4f){
				listener.onStartSwipeToDismissTransition(lastFlingVelocityY);
			}else{
				animatingCanceledDismiss=true;
				springBack();
			}
		}
		return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector){
		float factor=detector.getScaleFactor();
		matrix.postScale(factor, factor, detector.getFocusX()-getWidth()/2f, detector.getFocusY()-getHeight()/2f);
		updateViewTransform(false);
		lastScaleCenterX=detector.getFocusX()-getWidth()/2f;
		lastScaleCenterY=detector.getFocusY()-getHeight()/2f;
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector){
		requestDisallowInterceptTouchEvent(true);
		scaling=true;
		wasScaling=true;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector){
		scaling=false;
		updateLimits(child.getScaleX());
		transX=child.getTranslationX();
		transY=child.getTranslationY();
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e){
		listener.onSingleTap();
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e){
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e){
		if(e.getAction()==MotionEvent.ACTION_UP){
			if(e.getEventTime()-e.getDownTime()<ViewConfiguration.getTapTimeout()){
				if(animatingTransform)
					return false;
				if(child.getScaleX()<maxScale){
					float scale=maxScale/child.getScaleX();
					matrix.postScale(scale, scale, e.getX()-getWidth()/2f, e.getY()-getHeight()/2f);
					matrix.getValues(matrixValues);
					transX=matrixValues[Matrix.MTRANS_X];
					transY=matrixValues[Matrix.MTRANS_Y];
					updateLimits(maxScale);
					clampMatrixTranslationToLimits();
					updateViewTransform(true);
				}else{
					matrix.setScale(minScale, minScale);
					updateLimits(minScale);
					transX=transY=0;
					updateViewTransform(true);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e){
		totalScrollX=totalScrollY=0;
		lastFlingVelocityY=0;
		wasAnimatingTransition=false;
		if(!scroller.isFinished()){
			scroller.forceFinished(true);
			removeCallbacks(scrollerUpdater);
		}
		requestDisallowInterceptTouchEvent(true);
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e){}

	@Override
	public boolean onSingleTapUp(MotionEvent e){
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
		if(minTransY==maxTransY && minTransY==0f){
			if(minTransX==maxTransX && minTransX==0f){
				if(Math.abs(totalScrollY)>Math.abs(totalScrollX)){
					if(!swipingToDismiss){
						swipingToDismiss=true;
						matrix.postTranslate(-totalScrollX, 0);
						transX-=totalScrollX;
						listener.onStartSwipeToDismiss();
					}
					matrix.postTranslate(0, -distanceY);
					transY-=distanceY;
					updateViewTransform(false);
					float alpha=1f-Math.abs(transY)/getHeight();
					backgroundAlphaForTransition=alpha;
					listener.onSetBackgroundAlpha(alpha);
					return true;
				}
			}else{
				distanceY=0;
			}
		}
		totalScrollX-=distanceX;
		totalScrollY-=distanceY;
		matrix.postTranslate(-distanceX, -distanceY);
		transX-=distanceX;
		transY-=distanceY;
		boolean atEdge=false;
		if(transX<minTransX && canScrollRight){
			matrix.postTranslate(minTransX-transX, 0f);
			transX=minTransX;
			atEdge=true;
		}else if(transX>maxTransX && canScrollLeft){
			matrix.postTranslate(maxTransX-transX, 0f);
			transX=maxTransX;
			atEdge=true;
		}
		updateViewTransform(false);
		if(!scrolling){
			scrolling=true;
			// if the image is at the edge horizontally, or the user is dragging more vertically, intercept;
			// otherwise, give these touch events to the view pager to scroll pages
			requestDisallowInterceptTouchEvent(!atEdge || Math.abs(totalScrollX)<Math.abs(totalScrollY));
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e){}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
		if(swipingToDismiss){
			lastFlingVelocityY=velocityY;
			if(Math.abs(velocityY)>=V.dp(1000)){
				swipingToDismiss=false;
				scrolling=false;
				listener.onStartSwipeToDismissTransition(velocityY);
			}
		}else if(!animatingTransform){
			scroller.fling(Math.round(transX), Math.round(transY), Math.round(velocityX), Math.round(velocityY), Math.round(minTransX), Math.round(maxTransX), Math.round(minTransY), Math.round(maxTransY), 0, 0);
			postOnAnimation(scrollerUpdater);
		}
		return true;
	}

	private void doScrollerAnimation(){
		if(scroller.computeScrollOffset()){
			float dx=transX-scroller.getCurrX();
			float dy=transY-scroller.getCurrY();
			transX-=dx;
			transY-=dy;
			matrix.postTranslate(-dx, -dy);
			updateViewTransform(false);
			postOnAnimation(scrollerUpdater);
		}
	}

	public interface Listener{
		void onTransitionAnimationUpdate(float translateX, float translateY, float scale);
		void onTransitionAnimationFinished();
		void onSetBackgroundAlpha(float alpha);
		void onStartSwipeToDismiss();
		void onStartSwipeToDismissTransition(float velocityY);
		void onSwipeToDismissCanceled();
		void onDismissed();
		void onSingleTap();
	}
}
