package org.joinmastodon.android.ui.drawables;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RadialParticleSystemDrawable extends Drawable{
	private long particleLifetime;
	private int birthRate;
	private int startColor, endColor;
	private float velocity, velocityVariance;
	private float size;
	private ArrayList<Particle> activeParticles=new ArrayList<>(), nextActiveParticles=new ArrayList<>(), pool=new ArrayList<>();
	private int emitterX, emitterY;
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private float[] linearStartColor, linearEndColor;
	private long prevFrameTime;
	private Random rand=new Random();
	private Rect clipOutBounds=new Rect();

	public RadialParticleSystemDrawable(long particleLifetime, int birthRate, int startColor, int endColor, float velocity, float velocityVariance, float size){
		this.particleLifetime=particleLifetime;
		this.birthRate=birthRate;
		this.startColor=startColor;
		this.endColor=endColor;
		this.velocity=velocity;
		this.velocityVariance=velocityVariance;
		this.size=size;

		linearStartColor=new float[]{
				((startColor >> 24) & 0xFF)/255f,
				(float)Math.pow(((startColor >> 16) & 0xFF)/255f, 2.2),
				(float)Math.pow(((startColor >> 8) & 0xFF)/255f, 2.2),
				(float)Math.pow((startColor & 0xFF)/255f, 2.2)
		};
		linearEndColor=new float[]{
				((endColor >> 24) & 0xFF)/255f,
				(float)Math.pow(((endColor >> 16) & 0xFF)/255f, 2.2),
				(float)Math.pow(((endColor >> 8) & 0xFF)/255f, 2.2),
				(float)Math.pow((endColor & 0xFF)/255f, 2.2)
		};
	}

	@Override
	public void draw(@NonNull Canvas canvas){
		long now=SystemClock.uptimeMillis();
		nextActiveParticles.clear();
		for(Particle p:activeParticles){
			int time=(int)(now-p.birthTime);
			if(time>particleLifetime){
				pool.add(p);
				continue;
			}
			nextActiveParticles.add(p);
			float x=emitterX+time/1000f*p.velX;
			float y=emitterY+time/1000f*p.velY;
			if(clipOutBounds.contains((int)x, (int)y)){
				continue;
			}
			float fraction=time/(float)particleLifetime;
			paint.setColor(interpolateColor(fraction));
			canvas.drawCircle(x, y, size, paint);
		}
		long timeDiff=Math.min(100, now-prevFrameTime);
		int newParticleCount=Math.round(timeDiff/1000f*birthRate);
		for(int i=0;i<newParticleCount;i++){
			Particle p;
			if(!pool.isEmpty())
				p=pool.remove(pool.size()-1);
			else
				p=new Particle();
			p.birthTime=now;
			double angle=rand.nextDouble()*Math.PI*2;
			float vel=velocity+velocityVariance*(rand.nextFloat()*2-1f);
			p.velX=vel*(float)Math.cos(angle);
			p.velY=vel*(float)Math.sin(angle);
			nextActiveParticles.add(p);
		}
		ArrayList<Particle> tmp=nextActiveParticles;
		nextActiveParticles=activeParticles;
		activeParticles=tmp;
		invalidateSelf();
		prevFrameTime=now;
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

	public void setClipOutBounds(int l, int t, int r, int b){
		clipOutBounds.set(l, t, r, b);
	}

	private int interpolateColor(float fraction){
		float a=(linearStartColor[0]+(linearEndColor[0]-linearStartColor[0])*fraction)*255f;
		float r=(float)Math.pow(linearStartColor[1]+(linearEndColor[1]-linearStartColor[1])*fraction, 1.0/2.2)*255f;
		float g=(float)Math.pow(linearStartColor[2]+(linearEndColor[2]-linearStartColor[2])*fraction, 1.0/2.2)*255f;
		float b=(float)Math.pow(linearStartColor[3]+(linearEndColor[3]-linearStartColor[3])*fraction, 1.0/2.2)*255f;
		return (Math.round(a) << 24) | (Math.round(r) << 16) | (Math.round(g) << 8) | Math.round(b);

	}

	public void setEmitterPosition(int x, int y){
		emitterX=x;
		emitterY=y;
	}

	private static class Particle{
		public long birthTime;
		public float velX, velY;
	}
}
