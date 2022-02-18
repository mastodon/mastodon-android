package org.joinmastodon.android.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TileGridLayoutManager extends GridLayoutManager{
	private static final String TAG="TileGridLayoutManager";
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
}
