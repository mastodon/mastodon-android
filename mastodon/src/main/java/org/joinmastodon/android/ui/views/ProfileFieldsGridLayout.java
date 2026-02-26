package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;

import me.grishka.appkit.utils.CustomViewHelper;

public class ProfileFieldsGridLayout extends ViewGroup implements CustomViewHelper{
	private int numColumns=2;
	private int maxRows=5;
	private int spacing;
	private int[] spanWidths=new int[numColumns];
	private Runnable layoutCallback;

	public ProfileFieldsGridLayout(Context context){
		this(context, null);
	}

	public ProfileFieldsGridLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public ProfileFieldsGridLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		spacing=dp(4);
	}

	@Override
	public void onViewAdded(View child){
		super.onViewAdded(child);
		child.setTag(R.id.profile_grid_layout_position, new ViewPosition());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int width=MeasureSpec.getSize(widthMeasureSpec);
		int availWidth=width-getPaddingLeft()-getPaddingRight();
		int totalHeight=0;
		int columnWidth=(availWidth-spacing*(numColumns-1))/numColumns;

		for(int i=0;i<numColumns-1;i++){
			spanWidths[i]=columnWidth*(i+1)+spacing*i;
		}
		spanWidths[numColumns-1]=availWidth;

		int columnIndex=0;
		int rowIndex=0;
		int rowHeight=0;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			LayoutParams lp=child.getLayoutParams();
			if(lp.height==LayoutParams.MATCH_PARENT)
				throw new IllegalArgumentException("Height must be wrap_content or a constant value");
			child.measure(MeasureSpec.AT_MOST | availWidth, lp.height>0 ? (MeasureSpec.EXACTLY | lp.height) : MeasureSpec.UNSPECIFIED);
			int childW=child.getMeasuredWidth();

			int columnsAvail=numColumns-columnIndex;
			int colSpan=1;
			for(int j=0;j<numColumns;j++){
				if(childW<=spanWidths[j]){
					colSpan=j+1;
					break;
				}
			}

			if(colSpan>columnsAvail){ // Start new row
				if(columnsAvail>0){
					// The previous view is last in its row, but there is still room. Stretch it to fill the row
					View prev=getChildAt(i-1);
					ViewPosition prevPos=(ViewPosition) prev.getTag(R.id.profile_grid_layout_position);
					prevPos.colSpan+=columnsAvail;
				}

				columnIndex=0;
				totalHeight+=rowHeight+spacing;
				rowHeight=child.getMeasuredHeight();
				rowIndex++;
				columnsAvail=numColumns;
			}else{
				rowHeight=Math.max(rowHeight, child.getMeasuredHeight());
			}

			if(i==getChildCount()-1){ // Make sure the last view fills its row
				colSpan=columnsAvail;
			}

			ViewPosition pos=(ViewPosition) child.getTag(R.id.profile_grid_layout_position);
			pos.column=columnIndex;
			pos.row=rowIndex;
			pos.colSpan=colSpan;

			columnIndex+=colSpan;
		}
		totalHeight+=rowHeight;
		setMeasuredDimension(width, totalHeight);
		if(layoutCallback!=null)
			layoutCallback.run();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		int yOffset=0;
		int currentRow=0;
		int rowHeight=0;
		int availWidth=(r-l)-getPaddingLeft()-getPaddingRight();
		int columnWidth=(availWidth-spacing*(numColumns-1))/numColumns;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			ViewPosition pos=(ViewPosition) child.getTag(R.id.profile_grid_layout_position);
			if(pos.row>0 && pos.row!=currentRow){
				yOffset+=rowHeight+spacing;
				rowHeight=0;
				currentRow++;
			}
			// Measure again, because some views misbehave if layout dimensions don't match their measured dimensions
			child.measure(MeasureSpec.EXACTLY | spanWidths[pos.colSpan-1], MeasureSpec.EXACTLY | child.getMeasuredHeight());
			int x=(columnWidth+spacing)*pos.column;
			child.layout(x, yOffset, x+spanWidths[pos.colSpan-1], yOffset+child.getMeasuredHeight());
			rowHeight=Math.max(rowHeight, child.getMeasuredHeight());
		}
	}

	public void setLayoutCallback(Runnable layoutCallback){
		this.layoutCallback=layoutCallback;
	}

	public void setColumnCount(int columns){
		if(numColumns!=columns){
			numColumns=columns;
			spanWidths=new int[columns];
			requestLayout();
		}
	}

	private static class ViewPosition{
		public int column, row, colSpan;
	}
}
