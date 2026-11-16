package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.RenderNode;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class RenderNodeDrawable extends Drawable{
	public final RenderNode renderNode=new RenderNode("qweqwe");

//	public RenderNodeDrawable(){
//		renderNode.setPivotX(0);
//		renderNode.setPivotY(0);
//	}

	@Override
	public void draw(@NonNull Canvas canvas){
		canvas.drawRenderNode(renderNode);
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
