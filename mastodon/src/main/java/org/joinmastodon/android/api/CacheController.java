package org.joinmastodon.android.api;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.requests.notifications.GetNotifications;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.Nullable;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.WorkerThread;

public class CacheController{
	private static final String TAG="CacheController";
	private static final int DB_VERSION=1;
	private static final WorkerThread databaseThread=new WorkerThread("databaseThread");
	private static final Handler uiHandler=new Handler(Looper.getMainLooper());

	private final String accountID;
	private DatabaseHelper db;
	private final Runnable databaseCloseRunnable=this::closeDatabase;

	static{
		databaseThread.start();
	}

	public CacheController(String accountID){
		this.accountID=accountID;
	}

	public void getHomeTimeline(String maxID, int count, Callback<List<Status>> callback){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				try(Cursor cursor=db.query("home_timeline", new String[]{"json"}, maxID==null ? null : "`id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`id` DESC", count+"")){
					if(cursor.getCount()==count){
						ArrayList<Status> result=new ArrayList<>();
						cursor.moveToFirst();
						do{
							Status status=MastodonAPIController.gson.fromJson(cursor.getString(0), Status.class);
							status.postprocess();
							result.add(status);
						}while(cursor.moveToNext());
						uiHandler.post(()->callback.onSuccess(result));
						return;
					}
				}catch(IOException x){
					Log.w(TAG, "getHomeTimeline: corrupted status object in database", x);
				}
				new GetHomeTimeline(maxID, null, count)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Status> result){
								callback.onSuccess(result);
								putHomeTimeline(result, maxID==null);
							}

							@Override
							public void onError(ErrorResponse error){
								callback.onError(error);
							}
						})
						.exec(accountID);
			}catch(SQLiteException x){
				Log.w(TAG, x);
				uiHandler.post(()->callback.onError(new MastodonErrorResponse(x.getLocalizedMessage())));
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	private void putHomeTimeline(List<Status> posts, boolean clear){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				if(clear)
					db.delete("home_timeline", null, null);
				ContentValues values=new ContentValues(2);
				for(Status s:posts){
					values.put("id", s.id);
					values.put("json", MastodonAPIController.gson.toJson(s));
					db.insertWithOnConflict("home_timeline", null, values, SQLiteDatabase.CONFLICT_REPLACE);
				}
			}catch(SQLiteException x){
				Log.w(TAG, x);
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	public void getNotifications(String maxID, int count, boolean onlyMentions, Callback<List<Notification>> callback){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				try(Cursor cursor=db.query(onlyMentions ? "notifications_mentions" : "notifications_all", new String[]{"json"}, maxID==null ? null : "`id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`id` DESC", count+"")){
					if(cursor.getCount()==count){
						ArrayList<Notification> result=new ArrayList<>();
						cursor.moveToFirst();
						do{
							Notification ntf=MastodonAPIController.gson.fromJson(cursor.getString(0), Notification.class);
							ntf.postprocess();
							result.add(ntf);
						}while(cursor.moveToNext());
						uiHandler.post(()->callback.onSuccess(result));
						return;
					}
				}catch(IOException x){
					Log.w(TAG, "getNotifications: corrupted notification object in database", x);
				}
				new GetNotifications(maxID, count, onlyMentions ? EnumSet.complementOf(EnumSet.of(Notification.Type.MENTION)): null)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Notification> result){
								callback.onSuccess(result);
								putNotifications(result, onlyMentions, maxID==null);
							}

							@Override
							public void onError(ErrorResponse error){
								callback.onError(error);
							}
						})
						.exec(accountID);
			}catch(SQLiteException x){
				Log.w(TAG, x);
				uiHandler.post(()->callback.onError(new MastodonErrorResponse(x.getLocalizedMessage())));
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	private void putNotifications(List<Notification> notifications, boolean onlyMentions, boolean clear){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				String table=onlyMentions ? "notifications_mentions" : "notifications_all";
				if(clear)
					db.delete(table, null, null);
				ContentValues values=new ContentValues(3);
				for(Notification n:notifications){
					values.put("id", n.id);
					values.put("json", MastodonAPIController.gson.toJson(n));
					values.put("type", n.type.ordinal());
					db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
				}
			}catch(SQLiteException x){
				Log.w(TAG, x);
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	private void closeDelayed(){
		databaseThread.postRunnable(databaseCloseRunnable, 10_000);
	}

	public void closeDatabase(){
		if(db!=null){
			if(BuildConfig.DEBUG)
				Log.d(TAG, "closeDatabase");
			db.close();
			db=null;
		}
	}

	private void cancelDelayedClose(){
		if(db!=null){
			databaseThread.handler.removeCallbacks(databaseCloseRunnable);
		}
	}

	private SQLiteDatabase getOrOpenDatabase(){
		if(db==null)
			db=new DatabaseHelper();
		return db.getWritableDatabase();
	}

	private class DatabaseHelper extends SQLiteOpenHelper{

		public DatabaseHelper(){
			super(MastodonApp.context, accountID+".db", null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db){
			db.execSQL("""
						CREATE TABLE `home_timeline` (
							`id` VARCHAR(25) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`flags` INTEGER NOT NULL DEFAULT 0
						)""");
			db.execSQL("""
						CREATE TABLE `notifications_all` (
							`id` VARCHAR(25) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`flags` INTEGER NOT NULL DEFAULT 0,
							`type` INTEGER NOT NULL
						)""");
			db.execSQL("""
						CREATE TABLE `notifications_mentions` (
							`id` VARCHAR(25) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`flags` INTEGER NOT NULL DEFAULT 0,
							`type` INTEGER NOT NULL
						)""");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

		}
	}
}
