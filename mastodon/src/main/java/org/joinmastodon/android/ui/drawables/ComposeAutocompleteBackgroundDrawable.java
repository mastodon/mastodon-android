package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class ComposeAutocompleteBackgroundDrawable extends Drawable{
	private Path path=new Path();
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private int fillColor, arrowOffset;

	public ComposeAutocompleteBackgroundDrawable(int fillColor){
		this.fillColor=fillColor;
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		canvas.save();
		canvas.translate(bounds.left, bounds.top);
		paint.setColor(0x80000000);
		canvas.drawPath(path, paint);
		canvas.translate(0, V.dp(1));
		paint.setColor(fillColor);
		canvas.drawPath(path, paint);
		int arrowSize=V.dp(10);
		canvas.drawRect(0, arrowSize, bounds.width(), bounds.height(), paint);
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

	public void setArrowOffset(int offset){
		arrowOffset=offset;
		updatePath();
		invalidateSelf();
	}

	@Override
	protected void onBoundsChange(Rect bounds){
		super.onBoundsChange(bounds);
		updatePath();
	}

	@Override
	public boolean getPadding(@NonNull Rect padding){
		padding.top=V.dp(11);
		return true;
	}

	private void updatePath(){
		path.rewind();
		int arrowSize=V.dp(10);
		path.moveTo(0, arrowSize*2);
		path.lineTo(0, arrowSize);
		path.lineTo(arrowOffset-arrowSize, arrowSize);
		path.lineTo(arrowOffset, 0);
		path.lineTo(arrowOffset+arrowSize, arrowSize);
		path.lineTo(getBounds().width(), arrowSize);
		path.lineTo(getBounds().width(), arrowSize*2);
		path.close();
	}
}
