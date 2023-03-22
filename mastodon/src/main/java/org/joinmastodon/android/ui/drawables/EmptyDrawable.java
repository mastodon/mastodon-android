package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EmptyDrawable extends Drawable{
	private final int width, height;

	public EmptyDrawable(int width, int height){
		this.width=width;
		this.height=height;
	}

	@Override
	public void draw(@NonNull Canvas canvas){

	}

	@Override
	public void setAlpha(int alpha){

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){

	}

	@Override
	public int getOpacity(){
		return PixelFormat.TRANSPARENT;
	}

	@Override
	public int getIntrinsicWidth(){
		return width;
	}

	@Override
	public int getIntrinsicHeight(){
		return height;
	}
}
