package org.joinmastodon.android;

import android.app.Application;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.UpdateTrack;

public class AppCenterWrapper{
	private static final String TAG="AppCenterWrapper";

	public static void init(Application app){
		if(AppCenter.isConfigured())
			return;
		Log.i(TAG, "initializing AppCenter SDK, build type is "+BuildConfig.BUILD_TYPE);

		if(BuildConfig.BUILD_TYPE.equals("appcenterPrivateBeta"))
			Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
		AppCenter.start(app, BuildConfig.appCenterKey, Distribute.class, Crashes.class);
	}
}
