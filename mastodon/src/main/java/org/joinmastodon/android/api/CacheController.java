package org.joinmastodon.android.api;

import android.content.ContentValues;
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
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.WorkerThread;

public class CacheController{
	private static final String TAG="CacheController";
	private static final int DB_VERSION=2;
	private static final WorkerThread databaseThread=new WorkerThread("databaseThread");
	private static final Handler uiHandler=new Handler(Looper.getMainLooper());

	private final String accountID;
	private DatabaseHelper db;
	private final Runnable databaseCloseRunnable=this::closeDatabase;

	private static final int POST_FLAG_GAP_AFTER=1;

	static{
		databaseThread.start();
	}

	public CacheController(String accountID){
		this.accountID=accountID;
	}

	public void getHomeTimeline(String maxID, int count, boolean forceReload, Callback<CacheablePaginatedResponse<List<Status>>> callback){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				List<Filter> filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(Filter.FilterContext.HOME)).collect(Collectors.toList());
				if(!forceReload){
					SQLiteDatabase db=getOrOpenDatabase();
					try(Cursor cursor=db.query("home_timeline", new String[]{"json", "flags"}, maxID==null ? null : "`id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`id` DESC", count+"")){
						if(cursor.getCount()==count){
							ArrayList<Status> result=new ArrayList<>();
							cursor.moveToFirst();
							String newMaxID;
							outer:
							do{
								Status status=MastodonAPIController.gson.fromJson(cursor.getString(0), Status.class);
								status.postprocess();
								int flags=cursor.getInt(1);
								status.hasGapAfter=((flags & POST_FLAG_GAP_AFTER)!=0);
								newMaxID=status.id;
								for(Filter filter:filters){
									if(filter.matches(status))
										continue outer;
								}
								result.add(status);
							}while(cursor.moveToNext());
							String _newMaxID=newMaxID;
							uiHandler.post(()->callback.onSuccess(new CacheablePaginatedResponse<>(result, _newMaxID, true)));
							return;
						}
					}catch(IOException x){
						Log.w(TAG, "getHomeTimeline: corrupted status object in database", x);
					}
				}
				new GetHomeTimeline(maxID, null, count, null)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Status> result){
								callback.onSuccess(new CacheablePaginatedResponse<>(result.stream().filter(new StatusFilterPredicate(filters)).collect(Collectors.toList()), result.isEmpty() ? null : result.get(result.size()-1).id, false));
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
				uiHandler.post(()->callback.onError(new MastodonErrorResponse(x.getLocalizedMessage(), 500, x)));
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	public void putHomeTimeline(List<Status> posts, boolean clear){
		runOnDbThread((db)->{
			if(clear)
				db.delete("home_timeline", null, null);
			ContentValues values=new ContentValues(3);
			for(Status s:posts){
				values.put("id", s.id);
				values.put("json", MastodonAPIController.gson.toJson(s));
				int flags=0;
				if(s.hasGapAfter)
					flags|=POST_FLAG_GAP_AFTER;
				values.put("flags", flags);
				db.insertWithOnConflict("home_timeline", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
		});
	}

	public void getNotifications(String maxID, int count, boolean onlyMentions, boolean forceReload, Callback<PaginatedResponse<List<Notification>>> callback){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				List<Filter> filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());
				if(!forceReload){
					SQLiteDatabase db=getOrOpenDatabase();
					try(Cursor cursor=db.query(onlyMentions ? "notifications_mentions" : "notifications_all", new String[]{"json"}, maxID==null ? null : "`id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`id` DESC", count+"")){
						if(cursor.getCount()==count){
							ArrayList<Notification> result=new ArrayList<>();
							cursor.moveToFirst();
							String newMaxID;
							outer:
							do{
								Notification ntf=MastodonAPIController.gson.fromJson(cursor.getString(0), Notification.class);
								ntf.postprocess();
								newMaxID=ntf.id;
								if(ntf.status!=null){
									for(Filter filter:filters){
										if(filter.matches(ntf.status))
											continue outer;
									}
								}
								result.add(ntf);
							}while(cursor.moveToNext());
							String _newMaxID=newMaxID;
							uiHandler.post(()->callback.onSuccess(new PaginatedResponse<>(result, _newMaxID)));
							return;
						}
					}catch(IOException x){
						Log.w(TAG, "getNotifications: corrupted notification object in database", x);
					}
				}
				new GetNotifications(maxID, count, onlyMentions ? EnumSet.of(Notification.Type.MENTION): EnumSet.allOf(Notification.Type.class))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Notification> result){
								callback.onSuccess(new PaginatedResponse<>(result.stream().filter(ntf->{
									if(ntf.status!=null){
										for(Filter filter:filters){
											if(filter.matches(ntf.status)){
												return false;
											}
										}
									}
									return true;
								}).collect(Collectors.toList()), result.isEmpty() ? null : result.get(result.size()-1).id));
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
				uiHandler.post(()->callback.onError(new MastodonErrorResponse(x.getLocalizedMessage(), 500, x)));
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	private void putNotifications(List<Notification> notifications, boolean onlyMentions, boolean clear){
		runOnDbThread((db)->{
			String table=onlyMentions ? "notifications_mentions" : "notifications_all";
			if(clear)
				db.delete(table, null, null);
			ContentValues values=new ContentValues(3);
			for(Notification n:notifications){
				if(n.type==null){
					continue;
				}
				values.put("id", n.id);
				values.put("json", MastodonAPIController.gson.toJson(n));
				values.put("type", n.type.ordinal());
				db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
		});
	}

	public void getRecentSearches(Consumer<List<SearchResult>> callback){
		runOnDbThread((db)->{
			try(Cursor cursor=db.query("recent_searches", new String[]{"json"}, null, null, null, null, "time DESC")){
				List<SearchResult> results=new ArrayList<>();
				while(cursor.moveToNext()){
					SearchResult result=MastodonAPIController.gson.fromJson(cursor.getString(0), SearchResult.class);
					result.postprocess();
					results.add(result);
				}
				uiHandler.post(()->callback.accept(results));
			}
		});
	}

	public void putRecentSearch(SearchResult result){
		runOnDbThread((db)->{
			ContentValues values=new ContentValues(4);
			values.put("id", result.getID());
			values.put("json", MastodonAPIController.gson.toJson(result));
			values.put("time", (int)(System.currentTimeMillis()/1000));
			db.insertWithOnConflict("recent_searches", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		});
	}

	public void deleteStatus(String id){
		runOnDbThread((db)->{
			db.delete("home_timeline", "`id`=?", new String[]{id});
		});
	}

	public void clearRecentSearches(){
		runOnDbThread((db)->db.delete("recent_searches", null, null));
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

	private void runOnDbThread(DatabaseRunnable r){
		runOnDbThread(r, null);
	}

	private void runOnDbThread(DatabaseRunnable r, Consumer<Exception> onError){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				r.run(db);
			}catch(SQLiteException|IOException x){
				Log.w(TAG, x);
				if(onError!=null)
					onError.accept(x);
			}finally{
				closeDelayed();
			}
		}, 0);
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
			createRecentSearchesTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			if(oldVersion==1){
				createRecentSearchesTable(db);
			}
		}

		private void createRecentSearchesTable(SQLiteDatabase db){
			db.execSQL("""
						CREATE TABLE `recent_searches` (
							`id` VARCHAR(50) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`time` INTEGER NOT NULL
						)""");
		}
	}

	@FunctionalInterface
	private interface DatabaseRunnable{
		void run(SQLiteDatabase db) throws IOException;
	}
}
