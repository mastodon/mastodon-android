package org.joinmastodon.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import java.lang.reflect.InvocationTargetException;

import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.NetworkUtils;

public class MastodonApp extends Application{

	@SuppressLint("StaticFieldLeak") // it's not a leak
	public static Context context;

	@Override
	public void onCreate(){
		super.onCreate();
		ImageCache.Parameters params=new ImageCache.Parameters();
		params.diskCacheSize=100*1024*1024;
		params.maxMemoryCacheSize=Integer.MAX_VALUE;
		ImageCache.setParams(params);
		NetworkUtils.setUserAgent("MastodonAndroid/"+BuildConfig.VERSION_NAME);
		context=getApplicationContext();

		// Call the appcenter SDK wrapper through reflection because it is only present in beta builds
		try{
			Class.forName("org.joinmastodon.android.AppCenterWrapper").getMethod("init", Application.class).invoke(null, this);
		}catch(ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException ignore){}
	}
}
