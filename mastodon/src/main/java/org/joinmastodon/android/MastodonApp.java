package org.joinmastodon.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class MastodonApp extends Application{

	@SuppressLint("StaticFieldLeak") // it's not a leak
	public static Context context;

	@Override
	public void onCreate(){
		super.onCreate();
		context=getApplicationContext();
	}
}
