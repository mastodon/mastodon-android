package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class CoverOverlayGradientDrawable extends Drawable{
	private LinearGradient gradient=new LinearGradient(0f, 0f, 0f, 100f, 0xB0000000, 0, Shader.TileMode.CLAMP);
	private Matrix gradientMatrix=new Matrix();
	private int topPadding, topOffset;
	private Paint paint=new Paint();

	public CoverOverlayGradientDrawable(){
		paint.setShader(gradient);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		gradientMatrix.setScale(1f, (bounds.height()-V.dp(40)-topPadding)/100f);
		gradientMatrix.postTranslate(0, topPadding+topOffset);
		gradient.setLocalMatrix(gradientMatrix);
		canvas.drawRect(bounds, paint);
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

	public void setTopPadding(int topPadding){
		this.topPadding=topPadding;
	}

	public void setTopOffset(int topOffset){
		this.topOffset=topOffset;
	}
}
