package org.joinmastodon.android.ui.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BlurHashDrawable extends Drawable{
	private final Bitmap bitmap;
	private final int width, height;
	private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);

	public BlurHashDrawable(Bitmap bitmap, int width, int height){
		this.bitmap=bitmap;
		this.width=width>0 ? width : bitmap.getWidth();
		this.height=height>0 ? height : bitmap.getHeight();
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		canvas.drawBitmap(bitmap, null, getBounds(), paint);
	}

	@Override
	public void setAlpha(int alpha){
		paint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){

	}

	@Override
	public int getOpacity(){
		return PixelFormat.OPAQUE;
	}

	@Override
	public int getIntrinsicWidth(){
		return width;
	}

	@Override
	public int getIntrinsicHeight(){
		return height;
	}

	@NonNull
	@Override
	public Drawable mutate(){
		return new BlurHashDrawable(bitmap, width, height);
	}
}
