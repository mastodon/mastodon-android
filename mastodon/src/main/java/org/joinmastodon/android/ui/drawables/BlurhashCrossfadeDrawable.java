package org.joinmastodon.android.ui.drawables;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatProperty;
import android.util.Property;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class BlurhashCrossfadeDrawable extends Drawable{

	private int width, height;
	private Drawable blurhashDrawable, imageDrawable;
	private float blurhashAlpha=1f;
	private ObjectAnimator currentAnim;

	private static Property<BlurhashCrossfadeDrawable, Float> BLURHASH_ALPHA;

	static{
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			BLURHASH_ALPHA=new FloatProperty<>(""){
				@Override
				public Float get(BlurhashCrossfadeDrawable object){
					return object.blurhashAlpha;
				}

				@Override
				public void setValue(BlurhashCrossfadeDrawable object, float value){
					object.blurhashAlpha=value;
					object.invalidateSelf();
				}
			};
		}else{
			BLURHASH_ALPHA=new Property<>(Float.class, ""){
				@Override
				public Float get(BlurhashCrossfadeDrawable object){
					return object.blurhashAlpha;
				}

				@Override
				public void set(BlurhashCrossfadeDrawable object, Float value){
					object.blurhashAlpha=value;
					object.invalidateSelf();
				}
			};
		}
	}

	public void setSize(int w, int h){
		width=w;
		height=h;
	}

	public void setBlurhashDrawable(Drawable blurhashDrawable){
		this.blurhashDrawable=blurhashDrawable;
		invalidateSelf();
	}

	public void setImageDrawable(Drawable imageDrawable){
		this.imageDrawable=imageDrawable;
		invalidateSelf();
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		if(imageDrawable!=null && blurhashAlpha<1f){
			imageDrawable.setBounds(getBounds());
			imageDrawable.draw(canvas);
		}
		if(blurhashDrawable!=null && blurhashAlpha>0f){
			blurhashDrawable.setBounds(getBounds());
			blurhashDrawable.setAlpha(Math.round(255*blurhashAlpha));
			blurhashDrawable.draw(canvas);
		}
	}

	@Override
	public void setAlpha(int alpha){

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter){

	}

	@Override
	public int getOpacity(){
		return PixelFormat.OPAQUE;
	}

	@Override
	public int getIntrinsicWidth(){
		if(width==0)
			return imageDrawable==null ? 1920 : imageDrawable.getIntrinsicWidth();
		return width;
	}

	@Override
	public int getIntrinsicHeight(){
		if(height==0)
			return imageDrawable==null ? 1080 : imageDrawable.getIntrinsicHeight();
		return height;
	}

	public void animateAlpha(float target){
		if(currentAnim!=null)
			currentAnim.cancel();
		ObjectAnimator anim=ObjectAnimator.ofFloat(this, BLURHASH_ALPHA, target);
		anim.setDuration(250);
		anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
		anim.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				currentAnim=null;
			}
		});
		anim.start();
		currentAnim=anim;
	}

	public void setCrossfadeAlpha(float alpha){
		blurhashAlpha=alpha;
		invalidateSelf();
	}
}
