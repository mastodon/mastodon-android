package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;

import java.util.ArrayList;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class InterpolatingMotionEffect implements SensorEventListener, View.OnTouchListener{

	private SensorManager sm;
	private WindowManager wm;
	private float[] rollBuffer = new float[9], pitchBuffer = new float[9];
	private int bufferOffset;
	private Sensor accelerometer;
	private boolean accelerometerEnabled;
	private ArrayList<ViewEffect> views=new ArrayList<>();
	private float pitch, roll;
	private float touchDownX, touchDownY, touchAddX, touchAddY, touchAddLastAnimX, touchAddLastAnimY;
	private PathInterpolator touchInterpolator=new PathInterpolator(0.5f, 1f, 0.89f, 1f);
	private SpringAnimation touchSpringX, touchSpringY;
	private FloatValueHolder touchSpringXHolder=new FloatValueHolder(){
		@Override
		public float getValue(){
			return touchAddX;
		}

		@Override
		public void setValue(float value){
			touchAddX=value;
			updateEffects();
		}
	};
	private FloatValueHolder touchSpringYHolder=new FloatValueHolder(){
		@Override
		public float getValue(){
			return touchAddY;
		}

		@Override
		public void setValue(float value){
			touchAddY=value;
			updateEffects();
		}
	};

	public InterpolatingMotionEffect(Context context){
		sm=context.getSystemService(SensorManager.class);
		wm=context.getSystemService(WindowManager.class);
		accelerometer=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	public void activate(){
		if(accelerometer==null || accelerometerEnabled)
			return;
		sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		accelerometerEnabled=true;
	}

	public void deactivate(){
		if(accelerometer==null || !accelerometerEnabled)
			return;
		sm.unregisterListener(this);
		accelerometerEnabled=false;
	}

	@Override
	public void onSensorChanged(SensorEvent event){
		int rotation=wm.getDefaultDisplay().getRotation();

		float x=event.values[0]/SensorManager.GRAVITY_EARTH;
		float y=event.values[1]/SensorManager.GRAVITY_EARTH;
		float z=event.values[2]/SensorManager.GRAVITY_EARTH;


		pitch=(float) (Math.atan2(x, Math.sqrt(y*y+z*z))/Math.PI*2.0);
		roll=(float) (Math.atan2(y, Math.sqrt(x*x+z*z))/Math.PI*2.0);

		switch(rotation){
			case Surface.ROTATION_0:
				break;
			case Surface.ROTATION_90:{
				float tmp=pitch;
				pitch=roll;
				roll=tmp;
				break;
			}
			case Surface.ROTATION_180:
				roll=-roll;
				pitch=-pitch;
				break;
			case Surface.ROTATION_270:{
				float tmp=-pitch;
				pitch=roll;
				roll=tmp;
				break;
			}
		}
		rollBuffer[bufferOffset]=roll;
		pitchBuffer[bufferOffset]=pitch;
		bufferOffset=(bufferOffset+1)%rollBuffer.length;
		roll=pitch=0;
		for(int i=0; i<rollBuffer.length; i++){
			roll+=rollBuffer[i];
			pitch+=pitchBuffer[i];
		}
		roll/=rollBuffer.length;
		pitch/=rollBuffer.length;
		if(roll>1f){
			roll=2f-roll;
		}else if(roll<-1f){
			roll=-2f-roll;
		}
		updateEffects();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){

	}

	public void addViewEffect(ViewEffect effect){
		views.add(effect);
	}

	public void removeViewEffect(ViewEffect effect){
		views.remove(effect);
	}

	public void removeAllViewEffects(){
		views.clear();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent ev){
		switch(ev.getAction()){
			case MotionEvent.ACTION_DOWN -> {
				if(touchSpringX!=null){
					touchAddLastAnimX=touchAddX;
					touchSpringX.cancel();
					touchSpringX=null;
				}else{
					touchAddLastAnimX=0;
				}
				if(touchSpringY!=null){
					touchAddLastAnimY=touchAddY;
					touchSpringY.cancel();
					touchSpringY=null;
				}else{
					touchAddLastAnimY=0;
				}
				touchDownX=ev.getX();
				touchDownY=ev.getY();
			}
			case MotionEvent.ACTION_MOVE -> {
				touchAddX=touchInterpolator.getInterpolation(Math.min(1f, Math.abs((ev.getX()-touchDownX)/(v.getWidth()/2f))));
				touchAddY=touchInterpolator.getInterpolation(Math.min(1f, Math.abs((ev.getY()-touchDownY)/(v.getHeight()/2f))));
				if(ev.getX()>touchDownX)
					touchAddX=-touchAddX;
				if(ev.getY()<touchDownY)
					touchAddY=-touchAddY;
				touchAddX+=touchAddLastAnimX;
				touchAddY+=touchAddLastAnimY;
				updateEffects();
			}
			case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				touchSpringX=new SpringAnimation(touchSpringXHolder, 0f);
				touchSpringX.setMinimumVisibleChange(0.01f);
				touchSpringX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(0.85f);
				touchSpringX.addEndListener((animation, canceled, value, velocity)->touchSpringX=null);
				touchSpringX.start();
				touchSpringY=new SpringAnimation(touchSpringYHolder, 0f);
				touchSpringY.setMinimumVisibleChange(0.01f);
				touchSpringY.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(0.85f);
				touchSpringY.addEndListener((animation, canceled, value, velocity)->touchSpringY=null);
				touchSpringY.start();
				updateEffects();
			}
		}
		return true;
	}

	private void updateEffects(){
		for(ViewEffect view:views){
			view.update(Math.min(1f, Math.max(-1f, pitch+touchAddX)), Math.min(1f, Math.max(-1f, roll+touchAddY)));
		}
	}

	public static class ViewEffect{
		private View view;
		private float minX, maxX, minY, maxY;

		public ViewEffect(View view, float minX, float maxX, float minY, float maxY){
			this.view=view;
			this.minX=minX;
			this.maxX=maxX;
			this.minY=minY;
			this.maxY=maxY;
		}

		private void update(float x, float y){
			view.setTranslationX(lerp(maxX, minX, (x+1f)/2f));
			view.setTranslationY(lerp(minY, maxY, (y+1f)/2f));
		}

		private static float lerp(float startValue, float endValue, float fraction) {
			return startValue + (fraction * (endValue - startValue));
		}
	}
}
