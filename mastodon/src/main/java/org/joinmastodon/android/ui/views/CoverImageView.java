package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class CoverImageView extends ImageView{
	private float imageTranslationY;

	public CoverImageView(Context context){
		super(context);
	}

	public CoverImageView(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public CoverImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas){
		canvas.save();
		canvas.translate(0, imageTranslationY);
		super.onDraw(canvas);
		canvas.restore();
	}

	public void setTransform(float transY){
		imageTranslationY=transY;
	}
}
