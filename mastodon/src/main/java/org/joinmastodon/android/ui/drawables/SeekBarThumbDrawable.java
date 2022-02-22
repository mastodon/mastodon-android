package org.joinmastodon.android.ui.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class SeekBarThumbDrawable extends Drawable{
	private Bitmap shadow1, shadow2;
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Context context;

	public SeekBarThumbDrawable(Context context){
		this.context=context;
		shadow1=Bitmap.createBitmap(V.dp(24), V.dp(24), Bitmap.Config.ALPHA_8);
		shadow2=Bitmap.createBitmap(V.dp(24), V.dp(24), Bitmap.Config.ALPHA_8);
		Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(0xFF000000);
		paint.setShadowLayer(V.dp(2), 0, V.dp(1), 0xFF000000);
		new Canvas(shadow1).drawCircle(V.dp(12), V.dp(12), V.dp(9), paint);
		paint.setShadowLayer(V.dp(3), 0, V.dp(1), 0xFF000000);
		new Canvas(shadow2).drawCircle(V.dp(12), V.dp(12), V.dp(9), paint);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		float centerX=getBounds().centerX();
		float centerY=getBounds().centerY();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(0x4d000000);
		canvas.drawBitmap(shadow1, centerX-shadow1.getWidth()/2f, centerY-shadow1.getHeight()/2f, paint);
		paint.setColor(0x26000000);
		canvas.drawBitmap(shadow2, centerX-shadow2.getWidth()/2f, centerY-shadow2.getHeight()/2f, paint);
		paint.setColor(UiUtils.getThemeColor(context, R.attr.colorButtonText));
		canvas.drawCircle(centerX, centerY, V.dp(7), paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(UiUtils.getThemeColor(context, R.attr.colorAccentLight));
		paint.setStrokeWidth(V.dp(4));
		canvas.drawCircle(centerX, centerY, V.dp(7), paint);
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
		return V.dp(24);
	}

	@Override
	public int getIntrinsicHeight(){
		return V.dp(24);
	}
}
