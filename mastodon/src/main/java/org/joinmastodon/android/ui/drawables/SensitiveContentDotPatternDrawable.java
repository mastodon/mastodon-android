package org.joinmastodon.android.ui.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import org.joinmastodon.android.MastodonApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class SensitiveContentDotPatternDrawable extends Drawable{
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

	public SensitiveContentDotPatternDrawable(int fgColor, int bgColor){
		int size=V.dp(8);
		float radius=MastodonApp.context.getResources().getDisplayMetrics().density;
		Bitmap bmp=Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas c=new Canvas(bmp);
		Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setColor(bgColor);
		c.drawRect(0, 0, size, size, p);
		p.setColor(fgColor);
		c.drawCircle(size/2f, size/2f, radius, p);
		paint.setShader(new BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
		paint.setAlpha(51);
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		canvas.drawRect(getBounds(), paint);
	}

	@Override
	public int getOpacity(){
		return 0;
	}

	@Override
	public void setAlpha(int alpha){

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){

	}
}
