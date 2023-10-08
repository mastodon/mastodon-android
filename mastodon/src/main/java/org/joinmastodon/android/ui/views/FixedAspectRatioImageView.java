package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedAspectRatioImageView extends ImageView{
	private float aspectRatio=1;
	private boolean useHeight;

	public FixedAspectRatioImageView(Context context){
		this(context, null);
	}

	public FixedAspectRatioImageView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public FixedAspectRatioImageView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(useHeight){
			int height=MeasureSpec.getSize(heightMeasureSpec);
			widthMeasureSpec=Math.round(height*aspectRatio) | MeasureSpec.EXACTLY;
		}else{
			int width=MeasureSpec.getSize(widthMeasureSpec);
			heightMeasureSpec=Math.round(width/aspectRatio) | MeasureSpec.EXACTLY;
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	public float getAspectRatio(){
		return aspectRatio;
	}

	public void setAspectRatio(float aspectRatio){
		this.aspectRatio=aspectRatio;
	}

	public boolean isUseHeight(){
		return useHeight;
	}

	public void setUseHeight(boolean useHeight){
		this.useHeight=useHeight;
	}
}
