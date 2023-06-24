package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class AudioAttachmentBackgroundDrawable extends Drawable{
	private int bgColor, wavesColor;
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private long[] animationStartTimes={0, 0};
	private boolean animationRunning;
	private Runnable[] restartRunnables={()->restartAnimation(0), ()->restartAnimation(1)};

	@Override
	public void draw(@NonNull Canvas canvas){
		Rect bounds=getBounds();
		paint.setColor(bgColor);
		canvas.drawRect(bounds, paint);

		float initialRadius=V.dp(48);
		float finalRadius=bounds.width()/2f;
		long time=SystemClock.uptimeMillis();
		boolean animationsStillRunning=false;

		for(int i=0;i<animationStartTimes.length;i++){
			 long t=time-animationStartTimes[i];
			 if(t<0)
				  continue;
			 float fraction=t/3000f;
			 if(fraction>1)
				  continue;
			 fraction=CubicBezierInterpolator.EASE_OUT.getInterpolation(fraction);
			 paint.setColor(wavesColor);
			 paint.setAlpha(Math.round(paint.getAlpha()*(1f-fraction)));
			 canvas.drawCircle(bounds.centerX(), bounds.centerY(), initialRadius+(finalRadius-initialRadius)*fraction, paint);
			 animationsStillRunning=true;
		}

		if(animationsStillRunning){
			 invalidateSelf();
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

	public void setColors(int bg, int waves){
		bgColor=bg;
		wavesColor=waves;
	}

	public void startAnimation(){
		if(animationRunning)
			return;

		long time=SystemClock.uptimeMillis();
		animationStartTimes[0]=time;
		scheduleSelf(restartRunnables[0], time+3000);
		scheduleSelf(restartRunnables[1], time+1500);
		animationRunning=true;
		invalidateSelf();
	}

	public void stopAnimation(boolean gracefully){
		if(!animationRunning)
			return;

		animationRunning=false;
		for(Runnable r:restartRunnables)
			unscheduleSelf(r);
		if(!gracefully){
			animationStartTimes[0]=animationStartTimes[1]=0;
		}
	}

	private void restartAnimation(int index){
		long time=SystemClock.uptimeMillis();
		animationStartTimes[index]=time;
		if(animationRunning)
			scheduleSelf(restartRunnables[index], time+3000);
	}
}
