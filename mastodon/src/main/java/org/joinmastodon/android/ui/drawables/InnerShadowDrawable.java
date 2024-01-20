package org.joinmastodon.android.ui.drawables;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InnerShadowDrawable extends Drawable{
	private int radius, color, offsetX, offsetY, cornerRadius;
	private Path path=new Path();
	private Paint shadowPaint=new Paint(Paint.ANTI_ALIAS_FLAG), bitmapShadowPaint=new Paint(Paint.ANTI_ALIAS_FLAG), bitmapPaint=new Paint();
	private RectF tmpRect=new RectF();
	private Bitmap bitmap;

	public InnerShadowDrawable(int cornerRadius, int blurRadius, int color, int offsetX, int offsetY){
		this.radius=blurRadius;
		this.color=color;
		this.offsetX=offsetX;
		this.offsetY=offsetY;
		this.cornerRadius=cornerRadius;
		shadowPaint.setColor(color);
		shadowPaint.setShadowLayer(blurRadius, offsetX, offsetY, color | 0xFF000000);
		bitmapPaint.setColor(color);
		bitmapShadowPaint.setColor(0xFF000000);
		bitmapShadowPaint.setShadowLayer(blurRadius, offsetX, offsetY, 0xFF000000);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		if(Build.VERSION.SDK_INT>=28 || !canvas.isHardwareAccelerated()){
			canvas.drawPath(path, shadowPaint);
		}else{
			if(bitmap==null){
				bitmap=Bitmap.createBitmap(getBounds().width(), getBounds().height(), Bitmap.Config.ALPHA_8);
				new Canvas(bitmap).drawPath(path, bitmapShadowPaint);
			}
			canvas.drawBitmap(bitmap, getBounds().left, getBounds().top, bitmapPaint);
		}
	}

	@Override
	public void setAlpha(int alpha){

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){

	}

	@Override
	public int getOpacity(){
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	protected void onBoundsChange(@NonNull Rect bounds){
		super.onBoundsChange(bounds);
		updatePath();
	}

	private void updatePath(){
		path.rewind();
		tmpRect.set(getBounds());
		tmpRect.inset(-100, -100);
		path.addRect(tmpRect, Path.Direction.CW);
		tmpRect.set(getBounds());
		path.addRoundRect(tmpRect, cornerRadius, cornerRadius, Path.Direction.CCW);
		bitmap=null;
	}
}
