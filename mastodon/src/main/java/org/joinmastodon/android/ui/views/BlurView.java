package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.joinmastodon.android.R;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class BlurView extends FrameLayout{
	private boolean blurEnabled;
	private float radius;
	private LegacyBlurImpl legacyImpl;

	public BlurView(Context context){
		this(context, null);
	}

	public BlurView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public BlurView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.BlurView);
		blurEnabled=ta.getBoolean(R.styleable.BlurView_blurEnabled, true);
		radius=ta.getDimensionPixelOffset(R.styleable.BlurView_android_radius, 0);
		ta.recycle();
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
			updateRenderEffect();
		else
			legacyImpl=new LegacyBlurImpl();
	}

	public void setBlurEnabled(boolean blurEnabled){
		this.blurEnabled=blurEnabled;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
			updateRenderEffect();
		else
			invalidate();
	}

	public void setRadius(float radius){
		this.radius=radius;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
			updateRenderEffect();
		else
			invalidate();
	}

	@RequiresApi(Build.VERSION_CODES.S)
	private void updateRenderEffect(){
		setRenderEffect(blurEnabled ? RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) : null);
	}

	@Override
	protected void dispatchDraw(@NonNull Canvas canvas){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S || !blurEnabled){
			super.dispatchDraw(canvas);
		}else{
			super.dispatchDraw(legacyImpl.getBitmapCanvas());
			canvas.drawBitmap(legacyImpl.getOutput(), 0, 0, null);
		}
	}

	private class LegacyBlurImpl{
		private Bitmap input, output;
		private Canvas canvas;
		private final ScriptIntrinsicBlur blur;
		private final RenderScript rs=RenderScript.create(getContext());
		private Allocation inputAllocation, outputAllocation;
		private float currentRadius=0;

		private LegacyBlurImpl(){
			blur=ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs));
		}

		private Canvas getBitmapCanvas(){
			if(input==null || input.getWidth()!=getWidth() || input.getHeight()!=getHeight()){
				input=Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
				output=Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
				canvas=new Canvas(input);
				inputAllocation=Allocation.createFromBitmap(rs, input);
				blur.setInput(inputAllocation);
				outputAllocation=Allocation.createFromBitmap(rs, output);
			}
			canvas.drawColor(0);
			return canvas;
		}

		private Bitmap getOutput(){
			if(currentRadius!=radius){
				currentRadius=radius;
				blur.setRadius(radius);
			}
			inputAllocation.copyFrom(input);
			blur.forEach(outputAllocation);
			outputAllocation.copyTo(output);
			return output;
		}
	}
}
