package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TiledDrawable extends Drawable{
	private final Drawable drawable;

	public TiledDrawable(Drawable drawable){
		this.drawable=drawable;
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		canvas.save();
		canvas.clipRect(bounds);
		int w=drawable.getIntrinsicWidth();
		int h=drawable.getIntrinsicHeight();
		for(int y=bounds.top;y<bounds.bottom;y+=h){
			for(int x=bounds.left;x<bounds.right;x+=w){
				drawable.setBounds(x, y, x+w, y+h);
				drawable.draw(canvas);
			}
		}
		canvas.restore();
	}

	@Override
	public void setAlpha(int alpha){
		drawable.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){
		drawable.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity(){
		return drawable.getOpacity();
	}
}
