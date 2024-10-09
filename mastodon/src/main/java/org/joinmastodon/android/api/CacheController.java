package org.joinmastodon.android.api;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.requests.lists.GetLists;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV1;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV2;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.NotificationGroup;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.WorkerThread;

public class CacheController{
	private static final String TAG="CacheController";
	private static final int DB_VERSION=5;
	public static final WorkerThread databaseThread=new WorkerThread("databaseThread");
	public static final Handler uiHandler=new Handler(Looper.getMainLooper());

	private final String accountID;
	private DatabaseHelper db;
	private final Runnable databaseCloseRunnable=this::closeDatabase;
	private boolean loadingNotifications;
	private final ArrayList<Callback<PaginatedResponse<List<NotificationViewModel>>>> pendingNotificationsCallbacks=new ArrayList<>();
	private List<FollowList> lists;

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
				if(!forceReload){
					SQLiteDatabase db=getOrOpenDatabase();
					try(Cursor cursor=db.query("home_timeline", new String[]{"json", "flags"}, maxID==null ? null : "`id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`time` DESC", count+"")){
						if(cursor.getCount()==count){
							ArrayList<Status> result=new ArrayList<>();
							cursor.moveToFirst();
							String newMaxID;
							do{
								Status status=MastodonAPIController.gson.fromJson(cursor.getString(0), Status.class);
								status.postprocess();
								int flags=cursor.getInt(1);
								status.hasGapAfter=((flags & POST_FLAG_GAP_AFTER)!=0);
								newMaxID=status.id;
								result.add(status);
							}while(cursor.moveToNext());
							String _newMaxID=newMaxID;
							AccountSessionManager.get(accountID).filterStatuses(result, FilterContext.HOME);
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
								ArrayList<Status> filtered=new ArrayList<>(result);
								AccountSessionManager.get(accountID).filterStatuses(filtered, FilterContext.HOME);
								callback.onSuccess(new CacheablePaginatedResponse<>(filtered, result.isEmpty() ? null : result.get(result.size()-1).id, false));
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
			ContentValues values=new ContentValues(4);
			for(Status s:posts){
				values.put("id", s.id);
				values.put("json", MastodonAPIController.gson.toJson(s));
				int flags=0;
				if(s.hasGapAfter)
					flags|=POST_FLAG_GAP_AFTER;
				values.put("flags", flags);
				values.put("time", s.createdAt.getEpochSecond());
				db.insertWithOnConflict("home_timeline", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
		});
	}

	private List<NotificationViewModel> makeNotificationViewModels(List<NotificationGroup> notifications, Map<String, Account> accounts, Map<String, Status> statuses){
		return notifications.stream()
				.filter(ng->ng.type!=null)
				.map(ng->{
					NotificationViewModel nvm=new NotificationViewModel();
					nvm.notification=ng;
					nvm.accounts=ng.sampleAccountIds.stream().map(accounts::get).collect(Collectors.toList());
					if(nvm.accounts.size()!=ng.sampleAccountIds.size())
						return null;
					if(ng.statusId!=null){
						nvm.status=statuses.get(ng.statusId);
						if(nvm.status==null)
							return null;
					}
					return nvm;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public void getNotifications(String maxID, int count, boolean onlyMentions, boolean forceReload, Callback<PaginatedResponse<List<NotificationViewModel>>> callback){
		cancelDelayedClose();
		databaseThread.postRunnable(()->{
			try{
				if(!forceReload){
					SQLiteDatabase db=getOrOpenDatabase();
					String suffix=onlyMentions ? "mentions" : "all";
					String table="notifications_"+suffix;
					String accountsTable="notifications_accounts_"+suffix;
					String statusesTable="notifications_statuses_"+suffix;
					try(Cursor cursor=db.query(table, new String[]{"json"}, maxID==null ? null : "`max_id`<?", maxID==null ? null : new String[]{maxID}, null, null, "`time` DESC", count+"")){
						if(cursor.getCount()==count){
							ArrayList<NotificationGroup> result=new ArrayList<>();
							cursor.moveToFirst();
							String newMaxID;
							HashSet<String> needAccounts=new HashSet<>(), needStatuses=new HashSet<>();
							do{
								NotificationGroup ntf=MastodonAPIController.gson.fromJson(cursor.getString(0), NotificationGroup.class);
								ntf.postprocess();
								newMaxID=ntf.pageMinId;
								needAccounts.addAll(ntf.sampleAccountIds);
								if(ntf.statusId!=null)
									needStatuses.add(ntf.statusId);
								result.add(ntf);
							}while(cursor.moveToNext());
							String _newMaxID=newMaxID;
							HashMap<String, Account> accounts=new HashMap<>();
							HashMap<String, Status> statuses=new HashMap<>();
							if(!needAccounts.isEmpty()){
								try(Cursor cursor2=db.query(accountsTable, new String[]{"json"}, "`id` IN ("+String.join(", ", Collections.nCopies(needAccounts.size(), "?"))+")",
										needAccounts.toArray(new String[0]), null, null, null)){
									while(cursor2.moveToNext()){
										Account acc=MastodonAPIController.gson.fromJson(cursor2.getString(0), Account.class);
										acc.postprocess();
										accounts.put(acc.id, acc);
									}
								}
							}
							if(!needStatuses.isEmpty()){
								try(Cursor cursor2=db.query(statusesTable, new String[]{"json"}, "`id` IN ("+String.join(", ", Collections.nCopies(needStatuses.size(), "?"))+")",
										needStatuses.toArray(new String[0]), null, null, null)){
									while(cursor2.moveToNext()){
										Status s=MastodonAPIController.gson.fromJson(cursor2.getString(0), Status.class);
										s.postprocess();
										statuses.put(s.id, s);
									}
								}
							}
							uiHandler.post(()->callback.onSuccess(new PaginatedResponse<>(makeNotificationViewModels(result, accounts, statuses), _newMaxID)));
							return;
						}
					}catch(IOException x){
						Log.w(TAG, "getNotifications: corrupted notification object in database", x);
					}
				}

				if(!onlyMentions && loadingNotifications){
					synchronized(pendingNotificationsCallbacks){
						pendingNotificationsCallbacks.add(callback);
					}
					return;
				}
				if(!onlyMentions)
					loadingNotifications=true;
				if(AccountSessionManager.get(accountID).getInstanceInfo().getApiVersion()>=2){
					new GetNotificationsV2(maxID, count, onlyMentions ? EnumSet.of(NotificationType.MENTION): EnumSet.allOf(NotificationType.class), NotificationType.getGroupableTypes())
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(GetNotificationsV2.GroupedNotificationsResults result){
									Map<String, Account> accounts=result.accounts.stream().collect(Collectors.toMap(a->a.id, Function.identity(), (a1, a2)->a2));
									Map<String, Status> statuses=result.statuses.stream().collect(Collectors.toMap(s->s.id, Function.identity(), (s1, s2)->s2));
									List<NotificationViewModel> notifications=makeNotificationViewModels(result.notificationGroups, accounts, statuses);
									databaseThread.postRunnable(()->putNotifications(result.notificationGroups, result.accounts, result.statuses, onlyMentions, maxID==null), 0);
									PaginatedResponse<List<NotificationViewModel>> res=new PaginatedResponse<>(notifications,
											result.notificationGroups.isEmpty() ? null : result.notificationGroups.get(result.notificationGroups.size()-1).pageMinId);
									callback.onSuccess(res);
									if(!onlyMentions){
										loadingNotifications=false;
										synchronized(pendingNotificationsCallbacks){
											for(Callback<PaginatedResponse<List<NotificationViewModel>>> cb:pendingNotificationsCallbacks){
												cb.onSuccess(res);
											}
											pendingNotificationsCallbacks.clear();
										}
									}
								}

								@Override
								public void onError(ErrorResponse error){
									callback.onError(error);
									if(!onlyMentions){
										loadingNotifications=false;
										synchronized(pendingNotificationsCallbacks){
											for(Callback<PaginatedResponse<List<NotificationViewModel>>> cb:pendingNotificationsCallbacks){
												cb.onError(error);
											}
											pendingNotificationsCallbacks.clear();
										}
									}
								}
							})
							.exec(accountID);
				}else{
					new GetNotificationsV1(maxID, count, onlyMentions ? EnumSet.of(NotificationType.MENTION): EnumSet.allOf(NotificationType.class))
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(List<Notification> result){
									ArrayList<Notification> filtered=new ArrayList<>(result);
									AccountSessionManager.get(accountID).filterStatusContainingObjects(filtered, n->n.status, FilterContext.NOTIFICATIONS);
									List<Status> statuses=filtered.stream().map(n->n.status).filter(Objects::nonNull).collect(Collectors.toList());
									List<Account> accounts=filtered.stream().map(n->n.account).collect(Collectors.toList());
									List<NotificationViewModel> converted=filtered.stream()
											.map(n->{
												NotificationGroup group=new NotificationGroup();
												group.groupKey="converted-"+n.id;
												group.notificationsCount=1;
												group.type=n.type;
												group.mostRecentNotificationId=group.pageMaxId=group.pageMinId=n.id;
												group.latestPageNotificationAt=n.createdAt;
												group.sampleAccountIds=List.of(n.account.id);
												group.event=n.event;
												group.moderationWarning=n.moderationWarning;
												if(n.status!=null)
													group.statusId=n.status.id;
												NotificationViewModel nvm=new NotificationViewModel();
												nvm.notification=group;
												nvm.status=n.status;
												nvm.accounts=List.of(n.account);
												return nvm;
											})
											.collect(Collectors.toList());
									PaginatedResponse<List<NotificationViewModel>> res=new PaginatedResponse<>(converted, result.isEmpty() ? null : result.get(result.size()-1).id);
									callback.onSuccess(res);
									if(!onlyMentions){
										loadingNotifications=false;
										synchronized(pendingNotificationsCallbacks){
											for(Callback<PaginatedResponse<List<NotificationViewModel>>> cb:pendingNotificationsCallbacks){
												cb.onSuccess(res);
											}
											pendingNotificationsCallbacks.clear();
										}
									}
									databaseThread.postRunnable(()->putNotifications(converted.stream().map(nvm->nvm.notification).collect(Collectors.toList()), accounts, statuses, onlyMentions, maxID==null), 0);
								}

								@Override
								public void onError(ErrorResponse error){
									callback.onError(error);
									if(!onlyMentions){
										loadingNotifications=false;
										synchronized(pendingNotificationsCallbacks){
											for(Callback<PaginatedResponse<List<NotificationViewModel>>> cb:pendingNotificationsCallbacks){
												cb.onError(error);
											}
											pendingNotificationsCallbacks.clear();
										}
									}
								}
							})
							.exec(accountID);
				}
			}catch(SQLiteException x){
				Log.w(TAG, x);
				uiHandler.post(()->callback.onError(new MastodonErrorResponse(x.getLocalizedMessage(), 500, x)));
			}finally{
				closeDelayed();
			}
		}, 0);
	}

	private void putNotifications(List<NotificationGroup> notifications, List<Account> accounts, List<Status> statuses, boolean onlyMentions, boolean clear){
		runOnDbThread((db)->{
			String suffix=onlyMentions ? "mentions" : "all";
			String table="notifications_"+suffix;
			String accountsTable="notifications_accounts_"+suffix;
			String statusesTable="notifications_statuses_"+suffix;
			if(clear){
				db.delete(table, null, null);
				db.delete(accountsTable, null, null);
				db.delete(statusesTable, null, null);
			}
			ContentValues values=new ContentValues(4);
			for(NotificationGroup n:notifications){
				if(n.type==null){
					continue;
				}
				values.put("id", n.groupKey);
				values.put("json", MastodonAPIController.gson.toJson(n));
				values.put("type", n.type.ordinal());
				values.put("time", n.latestPageNotificationAt.getEpochSecond());
				values.put("max_id", n.pageMaxId);
				db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
			values.clear();
			for(Account acc:accounts){
				values.put("id", acc.id);
				values.put("json", MastodonAPIController.gson.toJson(acc));
				db.insertWithOnConflict(accountsTable, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
			for(Status s:statuses){
				values.put("id", s.id);
				values.put("json", MastodonAPIController.gson.toJson(s));
				db.insertWithOnConflict(statusesTable, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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

	public void reloadLists(Callback<List<FollowList>> callback){
		new GetLists()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<FollowList> result){
						result.sort(Comparator.comparing(l->l.title));
						lists=result;
						if(callback!=null)
							callback.onSuccess(result);
						writeLists();
					}

					@Override
					public void onError(ErrorResponse error){
						if(callback!=null)
							callback.onError(error);
					}
				})
				.exec(accountID);
	}

	private List<FollowList> loadLists(){
		SQLiteDatabase db=getOrOpenDatabase();
		try(Cursor cursor=db.query("misc", new String[]{"value"}, "`key`=?", new String[]{"lists"}, null, null, null)){
			if(!cursor.moveToFirst())
				return null;
			return MastodonAPIController.gson.fromJson(cursor.getString(0), new TypeToken<List<FollowList>>(){}.getType());
		}
	}

	private void writeLists(){
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			values.put("key", "lists");
			values.put("value", MastodonAPIController.gson.toJson(lists));
			db.insertWithOnConflict("misc", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		});
	}

	public void getLists(Callback<List<FollowList>> callback){
		if(lists!=null){
			if(callback!=null)
				callback.onSuccess(lists);
			return;
		}
		databaseThread.postRunnable(()->{
			List<FollowList> lists=loadLists();
			if(lists!=null){
				this.lists=lists;
				if(callback!=null)
					uiHandler.post(()->callback.onSuccess(lists));
				return;
			}
			reloadLists(callback);
		}, 0);
	}

	public void addList(FollowList list){
		if(lists==null)
			return;
		lists.add(list);
		lists.sort(Comparator.comparing(l->l.title));
		writeLists();
	}

	public void deleteList(String id){
		if(lists==null)
			return;
		lists.removeIf(l->l.id.equals(id));
		writeLists();
	}

	public void updateList(FollowList list){
		if(lists==null)
			return;
		for(int i=0;i<lists.size();i++){
			if(lists.get(i).id.equals(list.id)){
				lists.set(i, list);
				lists.sort(Comparator.comparing(l->l.title));
				writeLists();
				break;
			}
		}
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
							`flags` INTEGER NOT NULL DEFAULT 0,
							`time` INTEGER NOT NULL
						)""");
			createNotificationsTables(db, "all");
			createNotificationsTables(db, "mentions");
			createRecentSearchesTable(db);
			createMiscTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			if(oldVersion<2){
				createRecentSearchesTable(db);
			}
			if(oldVersion<3){
				addTimeColumns(db);
			}
			if(oldVersion<4){
				createMiscTable(db);
			}
			if(oldVersion<5){
				db.execSQL("DROP TABLE `notifications_all`");
				db.execSQL("DROP TABLE `notifications_mentions`");
				createNotificationsTables(db, "all");
				createNotificationsTables(db, "mentions");
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

		private void addTimeColumns(SQLiteDatabase db){
			db.execSQL("DELETE FROM `home_timeline`");
			db.execSQL("DELETE FROM `notifications_all`");
			db.execSQL("DELETE FROM `notifications_mentions`");
			db.execSQL("ALTER TABLE `home_timeline` ADD `time` INTEGER NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE `notifications_all` ADD `time` INTEGER NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE `notifications_mentions` ADD `time` INTEGER NOT NULL DEFAULT 0");
		}

		private void createMiscTable(SQLiteDatabase db){
			db.execSQL("""
						CREATE TABLE `misc` (
							`key` TEXT NOT NULL PRIMARY KEY,
							`value` TEXT
						)""");
		}

		private void createNotificationsTables(SQLiteDatabase db, String suffix){
			db.execSQL("CREATE TABLE `notifications_"+suffix+"` ("+
							"""
							`id` VARCHAR(100) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`flags` INTEGER NOT NULL DEFAULT 0,
							`type` INTEGER NOT NULL,
							`time` INTEGER NOT NULL,
							`max_id` VARCHAR(25) NOT NULL
						)""");
			db.execSQL("CREATE INDEX `notifications_"+suffix+"_max_id` ON `notifications_"+suffix+"`(`max_id`)");
			db.execSQL("CREATE TABLE `notifications_accounts_"+suffix+"` ("+
					"""
					`id` VARCHAR(25) NOT NULL PRIMARY KEY,
					`json` TEXT NOT NULL
				)""");
			db.execSQL("CREATE TABLE `notifications_statuses_"+suffix+"` ("+
					"""
					`id` VARCHAR(25) NOT NULL PRIMARY KEY,
					`json` TEXT NOT NULL
				)""");
		}
	}
}
