package org.joinmastodon.android.ui.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class PlayIconDrawable extends Drawable{
	private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path=new Path();

	public PlayIconDrawable(Context context){
		paint.setShadowLayer(V.dp(32), 0, 0, 0x80000000);
		paint.setColor(0xffffffff);
		path.moveTo(19.15f,32.5f);
		path.lineTo(32.5f,24.0f);
		path.lineTo(19.15f,15.5f);
		path.moveTo(24.0f,44.0f);
		path.quadTo(19.9f,44.0f,16.25f,42.42f);
		path.quadTo(12.6f,40.85f,9.88f,38.13f);
		path.quadTo(7.15f,35.4f,5.58f,31.75f);
		path.quadTo(4.0f,28.1f,4.0f,24.0f);
		path.quadTo(4.0f,19.85f,5.58f,16.2f);
		path.quadTo(7.15f,12.55f,9.88f,9.85f);
		path.quadTo(12.6f,7.15f,16.25f,5.58f);
		path.quadTo(19.9f,4.0f,24.0f,4.0f);
		path.quadTo(28.15f,4.0f,31.8f,5.58f);
		path.quadTo(35.45f,7.15f,38.15f,9.85f);
		path.quadTo(40.85f,12.55f,42.42f,16.2f);
		path.quadTo(44.0f,19.85f,44.0f,24.0f);
		path.quadTo(44.0f,28.1f,42.42f,31.75f);
		path.quadTo(40.85f,35.4f,38.15f,38.13f);
		path.quadTo(35.45f,40.85f,31.8f,42.42f);
		path.quadTo(28.15f,44.0f,24.0f,44.0f);

		Matrix matrix=new Matrix();
		float density=context.getResources().getDisplayMetrics().density;
		matrix.postScale(density*1.3333f, density*1.3333f);
		path.transform(matrix);
	}

	@Override
	public void draw(@NonNull Canvas c){
		c.save();
		Rect bounds=getBounds();
		c.translate(bounds.width()/2f-V.dp(32), bounds.height()/2f-V.dp(32));
		c.drawPath(path, paint);
		c.restore();
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
}
