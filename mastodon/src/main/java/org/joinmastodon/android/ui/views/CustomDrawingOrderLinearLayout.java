package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CustomDrawingOrderLinearLayout extends LinearLayout{
	private ChildDrawingOrderCallback drawingOrderCallback;

	public CustomDrawingOrderLinearLayout(Context context){
		this(context, null);
	}

	public CustomDrawingOrderLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CustomDrawingOrderLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		setChildrenDrawingOrderEnabled(true);
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int drawingPosition){
		if(drawingOrderCallback!=null)
			return drawingOrderCallback.getChildDrawingOrder(childCount, drawingPosition);
		return super.getChildDrawingOrder(childCount, drawingPosition);
	}

	public void setDrawingOrderCallback(ChildDrawingOrderCallback drawingOrderCallback){
		this.drawingOrderCallback=drawingOrderCallback;
	}
}
