package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.ui.PhotoLayoutHelper;

import java.util.Arrays;

import me.grishka.appkit.utils.V;

public class MediaGridLayout extends ViewGroup{
	private static final String TAG="MediaGridLayout";

	public static final int MAX_WIDTH=400; // dp
	private static final int GAP=2; // dp
	private PhotoLayoutHelper.TiledLayoutResult tiledLayout;
	private int[] columnStarts=new int[10], columnEnds=new int[10], rowStarts=new int[10], rowEnds=new int[10];

	public MediaGridLayout(Context context){
		this(context, null);
	}

	public MediaGridLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public MediaGridLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(tiledLayout==null){
			setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 0);
			return;
		}
		int width=Math.min(V.dp(MAX_WIDTH), MeasureSpec.getSize(widthMeasureSpec));
		int height=Math.round(width*(tiledLayout.height/(float)PhotoLayoutHelper.MAX_WIDTH));
		if(tiledLayout.width<PhotoLayoutHelper.MAX_WIDTH){
			width=Math.round(width*(tiledLayout.width/(float)PhotoLayoutHelper.MAX_WIDTH));
		}

		int offset=0;
		for(int i=0;i<tiledLayout.columnSizes.length;i++){
			columnStarts[i]=offset;
			offset+=Math.round(tiledLayout.columnSizes[i]/(float)tiledLayout.width*width);
			columnEnds[i]=offset;
			offset+=V.dp(GAP);
		}
		columnEnds[tiledLayout.columnSizes.length-1]=width;
		offset=0;
		for(int i=0;i<tiledLayout.rowSizes.length;i++){
			rowStarts[i]=offset;
			offset+=Math.round(tiledLayout.rowSizes[i]/(float)tiledLayout.height*height);
			rowEnds[i]=offset;
			offset+=V.dp(GAP);
		}
		rowEnds[tiledLayout.rowSizes.length-1]=height;

		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			LayoutParams lp=(LayoutParams) child.getLayoutParams();
			int colSpan=Math.max(1, lp.tile.colSpan)-1;
			int rowSpan=Math.max(1, lp.tile.rowSpan)-1;
			int w=columnEnds[lp.tile.startCol+colSpan]-columnStarts[lp.tile.startCol];
			int h=rowEnds[lp.tile.startRow+rowSpan]-rowStarts[lp.tile.startRow];
			child.measure(w | MeasureSpec.EXACTLY, h | MeasureSpec.EXACTLY);
		}

		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		if(tiledLayout==null)
			return;

		int maxWidth=V.dp(MAX_WIDTH);
		if(tiledLayout.width<PhotoLayoutHelper.MAX_WIDTH){
			maxWidth=Math.round((r-l)*(tiledLayout.width/(float)PhotoLayoutHelper.MAX_WIDTH));
		}
		int xOffset=0;
		if(r-l>maxWidth){
			xOffset=(r-l)/2-maxWidth/2;
		}

		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			LayoutParams lp=(LayoutParams) child.getLayoutParams();
			int colSpan=Math.max(1, lp.tile.colSpan)-1;
			int rowSpan=Math.max(1, lp.tile.rowSpan)-1;
			child.layout(columnStarts[lp.tile.startCol]+xOffset, rowStarts[lp.tile.startRow], columnEnds[lp.tile.startCol+colSpan]+xOffset, rowEnds[lp.tile.startRow+rowSpan]);
		}
	}

	public void setTiledLayout(PhotoLayoutHelper.TiledLayoutResult tiledLayout){
		this.tiledLayout=tiledLayout;
		requestLayout();
	}

	public static class LayoutParams extends ViewGroup.LayoutParams{
		public PhotoLayoutHelper.TiledLayoutResult.Tile tile;

		public LayoutParams(PhotoLayoutHelper.TiledLayoutResult.Tile tile){
			super(WRAP_CONTENT, WRAP_CONTENT);
			this.tile=tile;
		}
	}
}
