package org.joinmastodon.android.ui.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class SawtoothTearDrawable extends Drawable{
	private final Paint topPaint, bottomPaint;

	private static final int TOP_SAWTOOTH_HEIGHT=5;
	private static final int BOTTOM_SAWTOOTH_HEIGHT=3;
	private static final int STROKE_WIDTH=2;
	private static final int SAWTOOTH_PERIOD=14;

	public SawtoothTearDrawable(Context context){
		topPaint=makeShaderPaint(makeSawtoothTexture(context, TOP_SAWTOOTH_HEIGHT, SAWTOOTH_PERIOD, false, STROKE_WIDTH));
		bottomPaint=makeShaderPaint(makeSawtoothTexture(context, BOTTOM_SAWTOOTH_HEIGHT, SAWTOOTH_PERIOD, true, STROKE_WIDTH));
		Matrix matrix=new Matrix();
		//noinspection IntegerDivisionInFloatingPointContext
		matrix.setTranslate(V.dp(SAWTOOTH_PERIOD/2), 0);
		bottomPaint.getShader().setLocalMatrix(matrix);
	}

	private Bitmap makeSawtoothTexture(Context context, int height, int period, boolean fillBottom, int strokeWidth){
		int actualStrokeWidth=V.dp(strokeWidth);
		int actualPeriod=V.dp(period);
		int actualHeight=V.dp(height);
		Bitmap bitmap=Bitmap.createBitmap(actualPeriod, actualHeight+actualStrokeWidth*2, Bitmap.Config.ARGB_8888);
		Canvas c=new Canvas(bitmap);
		Path path=new Path();
		//noinspection SuspiciousNameCombination
		path.moveTo(-actualPeriod/2f, actualStrokeWidth);
		path.lineTo(0, actualHeight+actualStrokeWidth);
		//noinspection SuspiciousNameCombination
		path.lineTo(actualPeriod/2f, actualStrokeWidth);
		path.lineTo(actualPeriod, actualHeight+actualStrokeWidth);
		//noinspection SuspiciousNameCombination
		path.lineTo(actualPeriod*1.5f, actualStrokeWidth);
		if(fillBottom){
			path.lineTo(actualPeriod*1.5f, actualHeight*20);
			path.lineTo(-actualPeriod/2f, actualHeight*20);
		}else{
			path.lineTo(actualPeriod*1.5f, -actualHeight);
			path.lineTo(-actualPeriod/2f, -actualHeight);
		}
		path.close();
		Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(UiUtils.getThemeColor(context, R.attr.colorM3Surface));
		c.drawPath(path, paint);
		paint.setColor(UiUtils.getThemeColor(context, R.attr.colorM3OutlineVariant));
		paint.setStrokeWidth(actualStrokeWidth);
		paint.setStyle(Paint.Style.STROKE);
		c.drawPath(path, paint);
		return bitmap;
	}

	private Paint makeShaderPaint(Bitmap bitmap){
		BitmapShader shader=new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
		Paint paint=new Paint();
		paint.setShader(shader);
		return paint;
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		int strokeWidth=V.dp(STROKE_WIDTH);
		Rect bounds=getBounds();
		canvas.save();
		canvas.translate(bounds.left, bounds.top);
		canvas.drawRect(0, 0, bounds.width(), V.dp(TOP_SAWTOOTH_HEIGHT)+strokeWidth*2, topPaint);
		int bottomHeight=V.dp(BOTTOM_SAWTOOTH_HEIGHT)+strokeWidth*2;
		canvas.translate(0, bounds.height()-bottomHeight);
		canvas.drawRect(0, 0, bounds.width(), bottomHeight, bottomPaint);
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
}
