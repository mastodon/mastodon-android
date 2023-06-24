package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Switch;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class M3Switch extends Switch{
	private boolean ignoreRequestLayout;
	private DummyDrawable dummyDrawable=new DummyDrawable();

	public M3Switch(Context context){
		super(context);
	}

	public M3Switch(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public M3Switch(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		ignoreRequestLayout=true;
		Drawable prevThumbDrawable=getThumbDrawable();
		setThumbDrawable(dummyDrawable);
		ignoreRequestLayout=false;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		ignoreRequestLayout=true;
		setThumbDrawable(prevThumbDrawable);
		ignoreRequestLayout=false;
		try{
			Field fld=Switch.class.getDeclaredField("mThumbWidth");
			fld.setAccessible(true);
			fld.set(this, V.dp(32));
		}catch(Exception ignore){}
	}

	@Override
	public void requestLayout(){
		if(ignoreRequestLayout)
			return;
		super.requestLayout();
	}

	private static class DummyDrawable extends Drawable{

		@Override
		public void draw(@NonNull Canvas canvas){

		}

		@Override
		public void setAlpha(int alpha){

		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){

		}

		@Override
		public int getOpacity(){
			return 0;
		}

		@Override
		public int getIntrinsicWidth(){
			return V.dp(26);
		}
	}
}
