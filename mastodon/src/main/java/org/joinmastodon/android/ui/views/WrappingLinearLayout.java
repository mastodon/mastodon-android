package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;

import java.util.ArrayList;

/**
 * Something like a horizontal LinearLayout, but wraps child views onto a new line if they don't fit
 */
public class WrappingLinearLayout extends ViewGroup{
	private int verticalGap, horizontalGap;
	private ArrayList<Integer> rowHeights=new ArrayList<>();

	public WrappingLinearLayout(Context context){
		this(context, null);
	}

	public WrappingLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public WrappingLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.WrappingLinearLayout);
		verticalGap=ta.getDimensionPixelOffset(R.styleable.WrappingLinearLayout_android_verticalGap, 0);
		horizontalGap=ta.getDimensionPixelOffset(R.styleable.WrappingLinearLayout_android_horizontalGap, 0);
		ta.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int w=MeasureSpec.getSize(widthMeasureSpec)-getPaddingLeft()-getPaddingRight();
		int heightUsed=0, widthRemain=w, currentRowHeight=0;
		rowHeights.clear();
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			if(child.getVisibility()==GONE)
				continue;
			LayoutParams lp=child.getLayoutParams();
			int horizontalPadding=getPaddingLeft()+getPaddingRight();
			int verticalPadding=getPaddingTop()+getPaddingBottom();
			int horizontalMargins=0, verticalMargins=0;
			if(lp instanceof MarginLayoutParams mlp){
				horizontalPadding+=mlp.leftMargin+mlp.rightMargin;
				verticalPadding+=mlp.topMargin+mlp.bottomMargin;
				horizontalMargins=mlp.leftMargin+mlp.rightMargin;
				verticalMargins=mlp.topMargin+mlp.bottomMargin;
			}
			child.measure(getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width), getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height));
			currentRowHeight=Math.max(child.getMeasuredHeight()+verticalMargins, currentRowHeight);
			if(child.getMeasuredWidth()+(widthRemain<w ? horizontalGap : 0)+horizontalMargins>widthRemain){
				// Doesn't fit into the current row. Start a new one.
				heightUsed+=currentRowHeight+verticalGap;
				rowHeights.add(currentRowHeight);
				currentRowHeight=child.getMeasuredHeight()+verticalMargins;
				widthRemain=w;
			}else{
				// Does fit. Advance horizontally.
				if(widthRemain<w)
					widthRemain-=horizontalGap;
				widthRemain-=child.getMeasuredWidth()+horizontalMargins;
			}
		}
		// Take last row into account
		heightUsed+=currentRowHeight;
		rowHeights.add(currentRowHeight);
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), heightUsed+getPaddingTop()+getPaddingBottom());
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		if(rowHeights.isEmpty())
			return;
		boolean rtl=getLayoutDirection()==LAYOUT_DIRECTION_RTL;
		int xOffset=rtl ? getPaddingRight() : getPaddingLeft();
		int endPadding=rtl ? getPaddingLeft() : getPaddingRight();
		int yOffset=getPaddingTop();
		int w=getWidth()-getPaddingLeft()-getPaddingRight();
		int currentRowIndex=0;
		boolean firstInRow=true;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			if(child.getVisibility()==GONE)
				continue;
			int childW=child.getMeasuredWidth();
			int childH=child.getMeasuredHeight();
			int rowHeight=rowHeights.get(currentRowIndex);
			int childY, childX=xOffset;
			if(getWidth()-(xOffset+childW+(firstInRow ? 0 : horizontalGap))>=endPadding){
				xOffset+=childW+horizontalGap;
				if(child.getLayoutParams() instanceof MarginLayoutParams mlp){
					xOffset+=mlp.leftMargin+mlp.rightMargin;
				}
				firstInRow=false;
			}else if(currentRowIndex<rowHeights.size()-1){
				xOffset=rtl ? getPaddingRight() : getPaddingLeft();
				yOffset+=rowHeight+verticalGap;
				currentRowIndex++;
				childX=xOffset;
				rowHeight=rowHeights.get(currentRowIndex);
			}
			if(childH<rowHeight){
				childY=yOffset+rowHeight/2-childH/2;
			}else{
				childY=yOffset;
			}
			if(rtl){
				childX=getWidth()-childX-childW;
			}
			if(child.getLayoutParams() instanceof MarginLayoutParams mlp){
				childX+=rtl ? mlp.rightMargin : mlp.leftMargin;
				childY+=mlp.topMargin;
			}
			child.layout(childX, childY, childX+childW, childY+childH);
		}
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs){
		return new MarginLayoutParams(getContext(), attrs);
	}
}
