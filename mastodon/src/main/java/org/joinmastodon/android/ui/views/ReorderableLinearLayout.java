package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ReorderableLinearLayout extends LinearLayout{
	private static final String TAG="ReorderableLinearLayout";

	private View draggedView;
	private View bottomSibling, topSibling;
	private float startY;
	private OnDragListener dragListener;

	public ReorderableLinearLayout(Context context){
		super(context);
	}

	public ReorderableLinearLayout(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public ReorderableLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	public void startDragging(View child){
		getParent().requestDisallowInterceptTouchEvent(true);
		draggedView=child;
		draggedView.animate().translationZ(V.dp(1f)).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();

		int index=indexOfChild(child);
		if(index==-1)
			throw new IllegalArgumentException("view "+child+" is not a child of this layout");
		if(index>0)
			topSibling=getChildAt(index-1);
		if(index<getChildCount()-1)
			bottomSibling=getChildAt(index+1);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		if(draggedView!=null){
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
				draggedView=null;
				bottomSibling=null;
				topSibling=null;
			}else if(ev.getAction()==MotionEvent.ACTION_MOVE){
				draggedView.setTranslationY(ev.getY()-startY);
				if(topSibling!=null && draggedView.getY()<=topSibling.getY()){
					moveDraggedView(-1);
				}else if(bottomSibling!=null && draggedView.getY()>=bottomSibling.getY()){
					moveDraggedView(1);
				}
			}
		}
		return super.onTouchEvent(ev);
	}

	private void endDrag(){
		draggedView.animate().translationY(0f).translationZ(0f).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
	}

	private void moveDraggedView(int positionOffset){
		int index=indexOfChild(draggedView);
		int prevTop=draggedView.getTop();
		removeView(draggedView);
		int prevIndex=index;
		index+=positionOffset;
		addView(draggedView, index);
		final View prevSibling=positionOffset<0 ? topSibling : bottomSibling;
		int prevSiblingTop=prevSibling.getTop();
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
				float offset=prevTop-draggedView.getTop();
				startY-=offset;
				draggedView.setTranslationY(draggedView.getTranslationY()+offset);
				prevSibling.setTranslationY(prevSiblingTop-prevSibling.getTop());
				prevSibling.animate().translationY(0f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(200).start();
				return true;
			}
		});
	}

	public void setDragListener(OnDragListener dragListener){
		this.dragListener=dragListener;
	}

	public interface OnDragListener{
		void onSwapItems(int oldIndex, int newIndex);
	}
}
