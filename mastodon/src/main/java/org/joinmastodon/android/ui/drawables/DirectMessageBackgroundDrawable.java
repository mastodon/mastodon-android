package org.joinmastodon.android.ui.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

import org.joinmastodon.android.R;

public class DirectMessageBackgroundDrawable extends DrawableWrapper{
	public DirectMessageBackgroundDrawable(Context context){
		super(makeBitmapDrawable(context));
	}

	private static BitmapDrawable makeBitmapDrawable(Context context){
		Drawable d=context.getDrawable(R.drawable.bg_dm_pattern);
		Bitmap bmp=Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		d.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
		d.draw(new Canvas(bmp));
		BitmapDrawable bd=new BitmapDrawable(context.getResources(), bmp);
		bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		return bd;
	}
}
