package org.joinmastodon.android.updater;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Keep;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

@Keep
public class GithubSelfUpdaterImpl extends GithubSelfUpdater{
	private static final long CHECK_PERIOD=24*3600*1000L;
	private static final String TAG="GithubSelfUpdater";

	private UpdateState state=UpdateState.NO_UPDATE;
	private UpdateInfo info;
	private long downloadID;
	private BroadcastReceiver downloadCompletionReceiver=new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			if(downloadID!=0 && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)==downloadID){
				MastodonApp.context.unregisterReceiver(this);
				setState(UpdateState.DOWNLOADED);
			}
		}
	};

	public GithubSelfUpdaterImpl(){
		SharedPreferences prefs=getPrefs();
		int checkedByBuild=prefs.getInt("checkedByBuild", 0);
		if(prefs.contains("version") && checkedByBuild==BuildConfig.VERSION_CODE){
			info=new UpdateInfo();
			info.version=prefs.getString("version", null);
			info.size=prefs.getLong("apkSize", 0);
			downloadID=prefs.getLong("downloadID", 0);
			if(downloadID==0 || !getUpdateApkFile().exists()){
				state=UpdateState.UPDATE_AVAILABLE;
			}else{
				DownloadManager dm=MastodonApp.context.getSystemService(DownloadManager.class);
				state=dm.getUriForDownloadedFile(downloadID)==null ? UpdateState.DOWNLOADING : UpdateState.DOWNLOADED;
				if(state==UpdateState.DOWNLOADING){
					MastodonApp.context.registerReceiver(downloadCompletionReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
				}
			}
		}else if(checkedByBuild!=BuildConfig.VERSION_CODE && checkedByBuild>0){
			// We are in a new version, running for the first time after update. Gotta clean things up.
			long id=getPrefs().getLong("downloadID", 0);
			if(id!=0){
				MastodonApp.context.getSystemService(DownloadManager.class).remove(id);
			}
			getUpdateApkFile().delete();
			getPrefs().edit()
					.remove("apkSize")
					.remove("version")
					.remove("apkURL")
					.remove("checkedByBuild")
					.remove("downloadID")
					.apply();
		}
	}

	private SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("githubUpdater", Context.MODE_PRIVATE);
	}

	@Override
	public void maybeCheckForUpdates(){
		if(state!=UpdateState.NO_UPDATE && state!=UpdateState.UPDATE_AVAILABLE)
			return;
		long timeSinceLastCheck=System.currentTimeMillis()-getPrefs().getLong("lastCheck", 0);
		if(timeSinceLastCheck>CHECK_PERIOD || forceUpdate){
			setState(UpdateState.CHECKING);
			MastodonAPIController.runInBackground(this::actuallyCheckForUpdates);
		}
	}

	private void actuallyCheckForUpdates(){
		Request req=new Request.Builder()
				.url("https://api.github.com/repos/mastodon/mastodon-android/releases/latest")
				.build();
		Call call=MastodonAPIController.getHttpClient().newCall(req);
		try(Response resp=call.execute()){
			JsonObject obj=JsonParser.parseReader(resp.body().charStream()).getAsJsonObject();
			String tag=obj.get("tag_name").getAsString();
			Pattern pattern=Pattern.compile("v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
			Matcher matcher=pattern.matcher(tag);
			if(!matcher.find()){
				Log.w(TAG, "actuallyCheckForUpdates: release tag has wrong format: "+tag);
				return;
			}
			int newMajor=Integer.parseInt(matcher.group(1)), newMinor=Integer.parseInt(matcher.group(2)), newRevision=matcher.group(3)!=null ? Integer.parseInt(matcher.group(3)) : 0;
			Matcher curMatcher=pattern.matcher(BuildConfig.VERSION_NAME);
			if(!curMatcher.find()){
				Log.w(TAG, "actuallyCheckForUpdates: current version has wrong format: "+BuildConfig.VERSION_NAME);
				return;
			}
			int curMajor=Integer.parseInt(curMatcher.group(1)), curMinor=Integer.parseInt(curMatcher.group(2)), curRevision=matcher.group(3)!=null ? Integer.parseInt(curMatcher.group(3)) : 0;
			long newVersion=((long)newMajor << 32) | ((long)newMinor << 16) | newRevision;
			long curVersion=((long)curMajor << 32) | ((long)curMinor << 16) | curRevision;
			if(newVersion>curVersion || forceUpdate){
				forceUpdate=false;
				String version=newMajor+"."+newMinor;
				if(matcher.group(3)!=null)
					version+="."+newRevision;
				Log.d(TAG, "actuallyCheckForUpdates: new version: "+version);
				for(JsonElement el:obj.getAsJsonArray("assets")){
					JsonObject asset=el.getAsJsonObject();
					if("application/vnd.android.package-archive".equals(asset.get("content_type").getAsString()) && "uploaded".equals(asset.get("state").getAsString())){
						long size=asset.get("size").getAsLong();
						String url=asset.get("browser_download_url").getAsString();

						UpdateInfo info=new UpdateInfo();
						info.size=size;
						info.version=version;
						this.info=info;

						getPrefs().edit()
								.putLong("apkSize", size)
								.putString("version", version)
								.putString("apkURL", url)
								.putInt("checkedByBuild", BuildConfig.VERSION_CODE)
								.remove("downloadID")
								.apply();

						break;
					}
				}
			}
			getPrefs().edit().putLong("lastCheck", System.currentTimeMillis()).apply();
		}catch(Exception x){
			Log.w(TAG, "actuallyCheckForUpdates", x);
		}finally{
			setState(info==null ? UpdateState.NO_UPDATE : UpdateState.UPDATE_AVAILABLE);
		}
	}

	private void setState(UpdateState state){
		this.state=state;
		E.post(new SelfUpdateStateChangedEvent(state));
	}

	@Override
	public UpdateState getState(){
		return state;
	}

	@Override
	public UpdateInfo getUpdateInfo(){
		return info;
	}

	public File getUpdateApkFile(){
		return new File(MastodonApp.context.getExternalCacheDir(), "update.apk");
	}

	@Override
	public void downloadUpdate(){
		if(state==UpdateState.DOWNLOADING)
			throw new IllegalStateException();
		DownloadManager dm=MastodonApp.context.getSystemService(DownloadManager.class);
		MastodonApp.context.registerReceiver(downloadCompletionReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		downloadID=dm.enqueue(
				new DownloadManager.Request(Uri.parse(getPrefs().getString("apkURL", null)))
						.setDestinationUri(Uri.fromFile(getUpdateApkFile()))
		);
		getPrefs().edit().putLong("downloadID", downloadID).apply();
		setState(UpdateState.DOWNLOADING);
	}

	@Override
	public void installUpdate(Activity activity){
		if(state!=UpdateState.DOWNLOADED)
			throw new IllegalStateException();
		Uri uri;
		Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			uri=new Uri.Builder().scheme("content").authority(activity.getPackageName()+".self_update_provider").path("update.apk").build();
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}else{
			uri=Uri.fromFile(getUpdateApkFile());
		}
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		activity.startActivity(intent);

		// TODO figure out how to restart the app when updating via this new API
		/*
		PackageInstaller installer=activity.getPackageManager().getPackageInstaller();
		try{
			final int sid=installer.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
			installer.registerSessionCallback(new PackageInstaller.SessionCallback(){
				@Override
				public void onCreated(int i){

				}

				@Override
				public void onBadgingChanged(int i){

				}

				@Override
				public void onActiveChanged(int i, boolean b){

				}

				@Override
				public void onProgressChanged(int id, float progress){

				}

				@Override
				public void onFinished(int id, boolean success){
					activity.getPackageManager().setComponentEnabledSetting(new ComponentName(activity, AfterUpdateRestartReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				}
			});
			activity.getPackageManager().setComponentEnabledSetting(new ComponentName(activity, AfterUpdateRestartReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
			PackageInstaller.Session session=installer.openSession(sid);
			try(OutputStream out=session.openWrite("mastodon.apk", 0, info.size); InputStream in=new FileInputStream(getUpdateApkFile())){
				byte[] buffer=new byte[16384];
				int read;
				while((read=in.read(buffer))>0){
					out.write(buffer, 0, read);
				}
			}
//			PendingIntent intent=PendingIntent.getBroadcast(activity, 1, new Intent(activity, InstallerStatusReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
			PendingIntent intent=PendingIntent.getActivity(activity, 1, new Intent(activity, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
			session.commit(intent.getIntentSender());
		}catch(IOException x){
			Log.w(TAG, "installUpdate", x);
			Toast.makeText(activity, x.getMessage(), Toast.LENGTH_SHORT).show();
		}
		 */
	}

	@Override
	public float getDownloadProgress(){
		if(state!=UpdateState.DOWNLOADING)
			throw new IllegalStateException();
		DownloadManager dm=MastodonApp.context.getSystemService(DownloadManager.class);
		try(Cursor cursor=dm.query(new DownloadManager.Query().setFilterById(downloadID))){
			if(cursor.moveToFirst()){
				long loaded=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
				long total=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
//				Log.d(TAG, "getDownloadProgress: "+loaded+" of "+total);
				return total>0 ? (float)loaded/total : 0f;
			}
		}
		return 0;
	}

	@Override
	public void cancelDownload(){
		if(state!=UpdateState.DOWNLOADING)
			throw new IllegalStateException();
		DownloadManager dm=MastodonApp.context.getSystemService(DownloadManager.class);
		dm.remove(downloadID);
		downloadID=0;
		getPrefs().edit().remove("downloadID").apply();
		setState(UpdateState.UPDATE_AVAILABLE);
	}

	@Override
	public void handleIntentFromInstaller(Intent intent, Activity activity){
		int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0);
		if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
			Intent confirmIntent=intent.getParcelableExtra(Intent.EXTRA_INTENT);
			activity.startActivity(confirmIntent);
		}else if(status!=PackageInstaller.STATUS_SUCCESS){
			String msg=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
			Toast.makeText(activity, activity.getString(R.string.error)+":\n"+msg, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void reset(){
		getPrefs().edit().clear().apply();
		File apk=getUpdateApkFile();
		if(apk.exists())
			apk.delete();
		state=UpdateState.NO_UPDATE;
	}

	/*public static class InstallerStatusReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent){
			int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0);
			if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
				Intent confirmIntent=intent.getParcelableExtra(Intent.EXTRA_INTENT);
				context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}else if(status!=PackageInstaller.STATUS_SUCCESS){
				String msg=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
				Toast.makeText(context, context.getString(R.string.error)+":\n"+msg, Toast.LENGTH_LONG).show();
			}
		}
	}

	public static class AfterUpdateRestartReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent){
			if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())){
				context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, AfterUpdateRestartReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				Toast.makeText(context, R.string.update_installed, Toast.LENGTH_SHORT).show();
				Intent restartIntent=new Intent(context, MainActivity.class)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.setPackage(context.getPackageName());
				if(Build.VERSION.SDK_INT<Build.VERSION_CODES.P){
					context.startActivity(restartIntent);
				}else{
					// Bypass activity starting restrictions by starting it from a notification
					NotificationManager nm=context.getSystemService(NotificationManager.class);
					NotificationChannel chan=new NotificationChannel("selfUpdateRestart", context.getString(R.string.update_installed), NotificationManager.IMPORTANCE_HIGH);
					nm.createNotificationChannel(chan);
					Notification n=new Notification.Builder(context, "selfUpdateRestart")
							.setContentTitle(context.getString(R.string.update_installed))
							.setContentIntent(PendingIntent.getActivity(context, 1, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
							.setFullScreenIntent(PendingIntent.getActivity(context, 1, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE), true)
							.setSmallIcon(R.drawable.ic_ntf_logo)
							.build();
					nm.notify(1, n);
				}
			}
		}
	}*/
}
