package org.joinmastodon.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TileGridLayoutManager extends GridLayoutManager{
	private static final String TAG="TileGridLayoutManager";
	private int lastWidth=0;

	public TileGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public TileGridLayoutManager(Context context, int spanCount){
		super(context, spanCount);
	}

	public TileGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout){
		super(context, spanCount, orientation, reverseLayout);
	}

	@Override
	public int getColumnCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state){
		return 1;
	}

	@Override
	public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec){
		int width=View.MeasureSpec.getSize(widthSpec);
		// Is there a better way to invalidate item decorations when the size changes?
		if(lastWidth!=width){
			lastWidth=width;
			if(getChildCount()>0){
				((RecyclerView)getChildAt(0).getParent()).invalidateItemDecorations();
			}
		}
		super.onMeasure(recycler, state, widthSpec, heightSpec);
	}
}
