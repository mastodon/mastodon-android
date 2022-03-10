package org.joinmastodon.android.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

public class InterpolatingMotionEffect implements SensorEventListener{

	private SensorManager sm;
	private WindowManager wm;
	private float[] rollBuffer = new float[9], pitchBuffer = new float[9];
	private int bufferOffset;
	private Sensor accelerometer;
	private boolean accelerometerEnabled;
	private ArrayList<ViewEffect> views=new ArrayList<>();

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


		float pitch=(float) (Math.atan2(x, Math.sqrt(y*y+z*z))/Math.PI*2.0);
		float roll=(float) (Math.atan2(y, Math.sqrt(x*x+z*z))/Math.PI*2.0);

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
		for(ViewEffect view:views){
			view.update(pitch, roll);
		}
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
