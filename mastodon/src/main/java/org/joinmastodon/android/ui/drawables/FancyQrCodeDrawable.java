package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.google.zxing.common.BitMatrix;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FancyQrCodeDrawable extends Drawable{
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Path path=new Path(), scaledPath=new Path();
	private int size, logoOffset, logoSize;
	private Drawable logo;

	public FancyQrCodeDrawable(BitMatrix code, int color, Drawable logo){
		paint.setColor(color);
		this.logo=logo;
		size=code.getWidth();
		addMarker(0, 0);
		addMarker(size-7, 0);
		addMarker(0, size-7);
		float[] radii=new float[8];
		logoSize=size/3;
		if((size-logoSize)%2!=0){
			logoSize--;
		}
		logoOffset=(size-logoSize)/2;
		for(int y=0;y<logoSize;y++){
			for(int x=0;x<logoSize;x++){
				code.unset(x+logoOffset, y+logoOffset);
			}
		}
		for(int y=0;y<size;y++){
			for(int x=0;x<size;x++){
				// Skip corner markers because they turn out ugly with this algorithm
				if((x<7 && y<7) || (x>size-8 && y<7) || (x<7 && y>size-8)){
					continue;
				}

				if(code.get(x, y)){
					boolean t=y>0 && code.get(x, y-1);
					boolean b=y<size-1 && code.get(x, y+1);
					boolean l=x>0 && code.get(x-1, y);
					boolean r=x<size-1 && code.get(x+1, y);
					int neighborCount=(l ? 1 : 0)+(t ? 1 : 0)+(r ? 1 : 0)+(b ? 1 : 0);
					// Special-case optimizations
					if(neighborCount>=3 || (neighborCount==2 && ((l && r) || (t && b)))){ // 3 or 4 neighbors, or part of a straight line
						path.addRect(x, y, x+1, y+1, Path.Direction.CW);
						continue;
					}else if(neighborCount==0){ // No neighbors
						path.addCircle(x+0.5f, y+0.5f, 0.5f, Path.Direction.CW);
						continue;
					}
					Arrays.fill(radii, 0);
					if(l && t){ // round bottom-right corner
						radii[4]=radii[5]=1;
					}else if(t && r){ // round bottom-left corner
						radii[6]=radii[7]=1;
					}else if(r && b){ // round top-left corner
						radii[0]=radii[1]=1;
					}else if(b && l){ // round top-right corner
						radii[2]=radii[3]=1;
					}else if(l){ // right side
						radii[2]=radii[3]=radii[4]=radii[5]=0.5f;
					}else if(t){ // bottom side
						radii[4]=radii[5]=radii[6]=radii[7]=0.5f;
					}else if(r){ // left side
						radii[6]=radii[7]=radii[1]=radii[0]=0.5f;
					}else{ // top side
						radii[0]=radii[1]=radii[2]=radii[3]=0.5f;
					}
					path.addRoundRect(x, y, x+1, y+1, radii, Path.Direction.CW);
				}
			}
		}
	}

	private void addMarker(int x, int y){
		path.addRoundRect(x, y, x+7, y+7, 2.38f, 2.38f, Path.Direction.CW);
		path.addRoundRect(x+1, y+1, x+6, y+6, 1.33f, 1.33f, Path.Direction.CCW);
		path.addRoundRect(x+2, y+2, x+5, y+5, 0.8f, 0.8f, Path.Direction.CW);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		float factor=Math.min(bounds.width(), bounds.height())/(float)size;
		float xOff=0, yOff=0;
		float bw=bounds.width(), bh=bounds.height();
		if(bw>bh){
			xOff=bw/2f-bh/2f;
		}else if(bw<bh){
			yOff=bh/2f-bw/2f;
		}
		canvas.save();
		canvas.translate(-bounds.left+xOff, -bounds.top+yOff);
		canvas.drawPath(scaledPath, paint);
		int scaledOffset=Math.round((logoOffset+1)*factor);
		int scaledSize=Math.round((logoSize-2)*factor);
		logo.setBounds(scaledOffset, scaledOffset, scaledOffset+scaledSize, scaledOffset+scaledSize);
		logo.draw(canvas);
		canvas.restore();
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
	public int getIntrinsicWidth(){
		return size;
	}

	@Override
	public int getIntrinsicHeight(){
		return size;
	}

	@Override
	protected void onBoundsChange(@NonNull Rect bounds){
		super.onBoundsChange(bounds);
		float factor=Math.min(bounds.width(), bounds.height())/(float)size;
		scaledPath.rewind();
		Matrix matrix=new Matrix();
		matrix.setScale(factor, factor);
		scaledPath.addPath(path, matrix);
	}
}
