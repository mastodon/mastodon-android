package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;

import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.CustomViewHelper;
import me.grishka.appkit.utils.V;

public class ReorderableLinearLayout extends LinearLayout implements CustomViewHelper{
	private static final String TAG="ReorderableLinearLayout";

	private static final Interpolator sDragScrollInterpolator=t->t * t * t * t * t;

	private static final Interpolator sDragViewScrollCapInterpolator=t->{
		t -= 1.0f;
		return t * t * t * t * t + 1.0f;
	};
	private static final long DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS = 2000;

	private View draggedView;
	private View bottomSibling, topSibling;
	private float startX, startY, dX, dY, viewStartX, viewStartY;
	private OnDragListener dragListener;
	private boolean moveInBothDimensions;
	private int edgeSize;
	private View scrollableParent;
	private long dragScrollStartTime;
	private int cachedMaxScrollSpeed=-1;
	final Runnable scrollRunnable= new Runnable() {
		@Override
		public void run() {
			if (draggedView != null && scrollIfNecessary()) {
				if (draggedView != null) { //it might be lost during scrolling
//					moveIfNecessary(mSelected);
				}
				removeCallbacks(scrollRunnable);
				postOnAnimation(this);
			}
		}
	};

	public ReorderableLinearLayout(Context context){
		super(context);
	}

	public ReorderableLinearLayout(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public ReorderableLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
		edgeSize=dp(20);
	}

	public void startDragging(View child){
		getParent().requestDisallowInterceptTouchEvent(true);
		draggedView=child;
		dragListener.onDragStart(draggedView);

		int index=indexOfChild(child);
		if(index==-1)
			throw new IllegalArgumentException("view "+child+" is not a child of this layout");
		if(index>0)
			topSibling=getChildAt(index-1);
		if(index<getChildCount()-1)
			bottomSibling=getChildAt(index+1);

		scrollableParent=findScrollableParent(this);

		viewStartX=child.getX();
		viewStartY=child.getY();
	}

	private View findScrollableParent(View child){
		if(getOrientation()==VERTICAL){
			if(child.canScrollVertically(-1) || child.canScrollVertically(1))
				return child;
		}else{
			if(child.canScrollHorizontally(-1) || child.canScrollHorizontally(1))
				return child;
		}
		if(child.getParent() instanceof View v)
			return findScrollableParent(v);
		return null;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		if(draggedView!=null){
			startX=ev.getX();
			startY=ev.getY();
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev){
		if(draggedView!=null){
			if(ev.getAction()==MotionEvent.ACTION_UP || ev.getAction()==MotionEvent.ACTION_CANCEL){
				endDrag();
				removeCallbacks(scrollRunnable);
				draggedView=null;
				bottomSibling=null;
				topSibling=null;
			}else if(ev.getAction()==MotionEvent.ACTION_MOVE){
				dX=ev.getX()-startX;
				dY=ev.getY()-startY;

				if(moveInBothDimensions){
					draggedView.setTranslationX(dX);
					draggedView.setTranslationY(dY);
				}else if(getOrientation()==VERTICAL){
					draggedView.setTranslationY(dY);
				}else{
					draggedView.setTranslationX(dX);
				}

				removeCallbacks(scrollRunnable);
				scrollRunnable.run();

				if(getOrientation()==VERTICAL){
					if(topSibling!=null && draggedView.getY()<=topSibling.getY()){
						moveDraggedView(-1);
					}else if(bottomSibling!=null && draggedView.getY()>=bottomSibling.getY()){
						moveDraggedView(1);
					}
				}else{
					if(topSibling!=null && draggedView.getX()<=topSibling.getX()){
						moveDraggedView(-1);
					}else if(bottomSibling!=null && draggedView.getX()>=bottomSibling.getX()){
						moveDraggedView(1);
					}
				}
				dragListener.onDragMove(draggedView);
			}
		}
		return super.onTouchEvent(ev);
	}

	private void endDrag(){
		dragListener.onDragEnd(draggedView);
	}

	private void moveDraggedView(int positionOffset){
		int index=indexOfChild(draggedView);

		boolean isVertical=getOrientation()==VERTICAL;

		int prevOffset=isVertical ? draggedView.getTop() : draggedView.getLeft();
		removeView(draggedView);
		int prevIndex=index;
		index+=positionOffset;
		addView(draggedView, index);
		final View prevSibling=positionOffset<0 ? topSibling : bottomSibling;
		int prevSiblingOffset=isVertical ? prevSibling.getTop() : prevSibling.getLeft();
		if(index>0)
			topSibling=getChildAt(index-1);
		else
			topSibling=null;
		if(index<getChildCount()-1)
			bottomSibling=getChildAt(index+1);
		else
			bottomSibling=null;
		dragListener.onSwapItems(prevIndex, index);
		draggedView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				draggedView.getViewTreeObserver().removeOnPreDrawListener(this);
				float offset=prevOffset-(isVertical ? draggedView.getTop() : draggedView.getLeft());
				if(isVertical){
					startY-=offset;
					viewStartY-=offset;
					draggedView.setTranslationY(draggedView.getTranslationY()+offset);
					prevSibling.setTranslationY(prevSiblingOffset-prevSibling.getTop());
					prevSibling.animate().translationY(0f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(200).start();
				}else{
					startX-=offset;
					viewStartX-=offset;
					draggedView.setTranslationX(draggedView.getTranslationX()+offset);
					prevSibling.setTranslationX(prevSiblingOffset-prevSibling.getLeft());
					prevSibling.animate().translationX(0f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(200).start();
				}
				return true;
			}
		});
	}

	public void setDragListener(OnDragListener dragListener){
		this.dragListener=dragListener;
	}

	public boolean isMoveInBothDimensions(){
		return moveInBothDimensions;
	}

	public void setMoveInBothDimensions(boolean moveInBothDimensions){
		this.moveInBothDimensions=moveInBothDimensions;
	}

	boolean scrollIfNecessary(){
		if(draggedView==null || scrollableParent==null){
			dragScrollStartTime=Long.MIN_VALUE;
			return false;
		}
		final long now=System.currentTimeMillis();
		final long scrollDuration=dragScrollStartTime==Long.MIN_VALUE ? 0 : now-dragScrollStartTime;
		int scrollX=0;
		int scrollY=0;
		if(getOrientation()==HORIZONTAL){
			int curX=(int) (viewStartX+dX)-scrollableParent.getScrollX();
			final int leftDiff=curX-getPaddingLeft();
			if(dX<0 && leftDiff<0){
				scrollX=leftDiff;
			}else if(dX>0){
				final int rightDiff=curX+draggedView.getWidth()-(scrollableParent.getWidth()-getPaddingRight());
				if(rightDiff>0){
					scrollX=rightDiff;
				}
			}
		}else{
			int curY=(int) (viewStartY+dY)-scrollableParent.getScrollY();
			final int topDiff=curY-getPaddingTop();
			if(dY<0 && topDiff<0){
				scrollY=topDiff;
			}else if(dY>0){
				final int bottomDiff=curY+draggedView.getHeight()-(scrollableParent.getHeight()-getPaddingBottom());
				if(bottomDiff>0){
					scrollY=bottomDiff;
				}
			}
		}
		if(scrollX!=0){
			scrollX=interpolateOutOfBoundsScroll(draggedView.getWidth(), scrollX, scrollableParent.getWidth(), scrollDuration);
		}
		if(scrollY!=0){
			scrollY=interpolateOutOfBoundsScroll(draggedView.getHeight(), scrollY, scrollableParent.getHeight(), scrollDuration);
		}
		if(scrollX!=0 || scrollY!=0){
			if(dragScrollStartTime==Long.MIN_VALUE){
				dragScrollStartTime=now;
			}
			int prevX=scrollableParent.getScrollX();
			int prevY=scrollableParent.getScrollY();
			scrollableParent.scrollBy(scrollX, scrollY);
			draggedView.setTranslationX(draggedView.getTranslationX()-(scrollableParent.getScrollX()-prevX));
			draggedView.setTranslationY(draggedView.getTranslationY()-(scrollableParent.getScrollY()-prevY));
			return true;
		}
		dragScrollStartTime=Long.MIN_VALUE;
		return false;
	}

	public int interpolateOutOfBoundsScroll(int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll){
		final int maxScroll=getMaxDragScroll();
		final int absOutOfBounds=Math.abs(viewSizeOutOfBounds);
		final int direction=(int) Math.signum(viewSizeOutOfBounds);
		// might be negative if other direction
		float outOfBoundsRatio=Math.min(1f, 1f*absOutOfBounds/viewSize);
		final int cappedScroll=(int) (direction*maxScroll*sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio));
		final float timeRatio;
		if(msSinceStartScroll>DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS){
			timeRatio=1f;
		}else{
			timeRatio=(float) msSinceStartScroll/DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS;
		}
		final int value=(int) (cappedScroll*sDragScrollInterpolator.getInterpolation(timeRatio));
		if(value==0){
			return viewSizeOutOfBounds>0 ? 1 : -1;
		}
		return value;
	}

	private int getMaxDragScroll(){
		if(cachedMaxScrollSpeed==-1){
			cachedMaxScrollSpeed=getResources().getDimensionPixelSize(R.dimen.item_touch_helper_max_drag_scroll_per_frame);
		}
		return cachedMaxScrollSpeed;
	}

	public interface OnDragListener{
		void onSwapItems(int oldIndex, int newIndex);

		default void onDragStart(View view){
			view.animate().translationZ(V.dp(3f)).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
		}

		default void onDragEnd(View view){
			view.animate().translationY(0f).translationX(0f).translationZ(0f).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
		}

		default void onDragMove(View view){}
	}
}
