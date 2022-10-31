package org.joinmastodon.android.updater;

import android.app.Activity;
import android.content.Intent;

import org.joinmastodon.android.BuildConfig;

public abstract class GithubSelfUpdater{
	private static GithubSelfUpdater instance;

	public static GithubSelfUpdater getInstance(){
		if(instance==null){
			try{
				Class<?> c=Class.forName("org.joinmastodon.android.updater.GithubSelfUpdaterImpl");
				instance=(GithubSelfUpdater) c.newInstance();
			}catch(IllegalAccessException|InstantiationException|ClassNotFoundException ignored){
			}
		}
		return instance;
	}

	public static boolean needSelfUpdating(){
		return BuildConfig.BUILD_TYPE.equals("githubRelease");
	}

	public abstract void maybeCheckForUpdates();

	public abstract GithubSelfUpdater.UpdateState getState();

	public abstract GithubSelfUpdater.UpdateInfo getUpdateInfo();

	public abstract void downloadUpdate();

	public abstract void installUpdate(Activity activity);

	public abstract float getDownloadProgress();

	public abstract void cancelDownload();

	public abstract void handleIntentFromInstaller(Intent intent, Activity activity);

	public enum UpdateState{
		NO_UPDATE,
		CHECKING,
		UPDATE_AVAILABLE,
		DOWNLOADING,
		DOWNLOADED
	}

	public static class UpdateInfo{
		public String version;
		public long size;
	}
}
