package org.joinmastodon.android.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;

/**
 * A modification of WrappingLinearLayout that hides child views that don't fit in a single row and shows a button to reveal them.
 */
public class ExpandableWrappingLinearLayout extends WrappingLinearLayout{
	private boolean expanded=false;

	public ExpandableWrappingLinearLayout(Context context){
		this(context, null);
	}

	public ExpandableWrappingLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public ExpandableWrappingLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int w=MeasureSpec.getSize(widthMeasureSpec)-getPaddingLeft()-getPaddingRight();
		int heightUsed=0, widthRemain=w, currentRowHeight=0;
		rowHeights.clear();

		TextView expandButton=findViewById(R.id.expand_button);
		if(expandButton!=null){
			expandButton.setVisibility(expanded ? GONE : VISIBLE);
			// make sure the button is the last view
			removeViewInLayout(expandButton);
			addViewInLayout(expandButton, getChildCount(), expandButton.getLayoutParams());
		}
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			if(child!=expandButton)
				child.setVisibility(VISIBLE);
		}

		boolean didHide=false;

		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			if(child.getVisibility()==GONE)
				continue;
			if(!didHide && child==expandButton){
				child.setVisibility(GONE);
				continue;
			}
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
				if(!expanded){
					if(expandButton!=null && !didHide){
						for(int j=i;j<getChildCount();j++){
							child=getChildAt(j);
							if(child!=expandButton)
								child.setVisibility(GONE);
						}
						if(widthRemain<w)
							widthRemain-=horizontalGap;
						widthRemain-=child.getMeasuredWidth()+horizontalMargins;

						int remainingCount=getChildCount()-1-i;
						expandButton.setText(String.format("+%,d", remainingCount));
						lp=expandButton.getLayoutParams();
						expandButton.measure(getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width), getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height));
						if(expandButton.getMeasuredWidth()+horizontalMargins>widthRemain && i>0){ // Didn't fit. Remove one view to make room and measure again
							getChildAt(i-1).setVisibility(GONE);
							remainingCount++;
							expandButton.setText(String.format("+%,d", remainingCount));
							expandButton.measure(getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width), getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height));
						}
						didHide=true;
					}
					continue;
				}
				// Doesn't fit into the current row. Start a new one.
				heightUsed+=currentRowHeight+verticalGap;
				rowHeights.add(currentRowHeight);
				currentRowHeight=child.getMeasuredHeight()+verticalMargins;
				widthRemain=w-child.getMeasuredWidth()+horizontalMargins;
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

	/*@Override
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
		TextView expandButton=findViewById(R.id.expand_button);
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
				xOffset+=childW+horizontalGap;
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
	}*/

	public void expand(){
		expanded=true;
		requestLayout();
	}
}
