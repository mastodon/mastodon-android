package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class VideoPlayerSeekBarThumbDrawable extends Drawable{
	private Paint thumbPaint=new Paint(Paint.ANTI_ALIAS_FLAG), clearPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Path clearPath=new Path();

	public VideoPlayerSeekBarThumbDrawable(){
		thumbPaint.setColor(0xffffffff);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		clearPath.addRect(0, 0, V.dp(20), V.dp(32), Path.Direction.CW);
		Path tmp=new Path();
		float radius=V.dp(2);
		tmp.addRoundRect(V.dp(-2), V.dp(12), V.dp(2), V.dp(20), radius, radius, Path.Direction.CW);
		tmp.addRoundRect(V.dp(18), V.dp(12), V.dp(22), V.dp(20), radius, radius, Path.Direction.CW);
		clearPath.op(tmp, Path.Op.DIFFERENCE);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		int thumbWidth=V.dp(4), thumbHeight=V.dp(32);
		int thumbX=bounds.centerX()-thumbWidth/2, thumbY=bounds.centerY()-thumbHeight/2;
		canvas.save();
		canvas.translate(thumbX-V.dp(8), thumbY);
		canvas.drawPath(clearPath, clearPaint);
		canvas.restore();
		canvas.drawRoundRect(thumbX, thumbY, thumbX+thumbWidth, thumbY+thumbHeight, V.dp(2), V.dp(2), thumbPaint);
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
		return V.dp(8);
	}

	@Override
	public int getIntrinsicHeight(){
		return V.dp(32);
	}
}
