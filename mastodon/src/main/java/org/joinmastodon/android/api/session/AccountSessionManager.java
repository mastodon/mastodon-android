package org.joinmastodon.android.api.session;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.DatabaseRunnable;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.WrapperRequest;
import org.joinmastodon.android.api.gson.JsonObjectBuilder;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.filters.GetLegacyFilters;
import org.joinmastodon.android.api.requests.instance.GetCustomEmojis;
import org.joinmastodon.android.api.requests.instance.GetInstanceV1;
import org.joinmastodon.android.api.requests.instance.GetInstanceV2;
import org.joinmastodon.android.api.requests.oauth.CreateOAuthApp;
import org.joinmastodon.android.events.EmojiUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.InstanceV1;
import org.joinmastodon.android.model.InstanceV2;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.Token;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountSessionManager{
	private static final String TAG="AccountSessionManager";
	public static final String SCOPE="read write follow push";
	public static final String REDIRECT_URI="mastodon-android-auth://callback";
	private static final int DB_VERSION=3;

	private static final AccountSessionManager instance=new AccountSessionManager();

	private HashMap<String, AccountSession> sessions=new HashMap<>();
	private HashMap<String, List<EmojiCategory>> customEmojis=new HashMap<>();
	private HashMap<String, Long> instancesLastUpdated=new HashMap<>();
	private HashMap<String, Instance> instances=new HashMap<>();
	private MastodonAPIController unauthenticatedApiController=new MastodonAPIController(null);
	private Instance authenticatingInstance;
	private Application authenticatingApp;
	private String lastActiveAccountID;
	private SharedPreferences prefs;
	private boolean loadedInstances;
	private DatabaseHelper db;
	private final Runnable databaseCloseRunnable=this::closeDatabase;
	private final Object databaseLock=new Object();

	public static AccountSessionManager getInstance(){
		return instance;
	}

	private AccountSessionManager(){
		prefs=MastodonApp.context.getSharedPreferences("account_manager", Context.MODE_PRIVATE);
		runWithDatabase(db->{
			HashSet<String> domains=new HashSet<>();
			try(Cursor cursor=db.query("accounts", null, null, null, null, null, null)){
				ContentValues values=new ContentValues();
				while(cursor.moveToNext()){
					DatabaseUtils.cursorRowToContentValues(cursor, values);
					AccountSession session=new AccountSession(values);
					domains.add(session.domain.toLowerCase());
					sessions.put(session.getID(), session);
				}
			}
			readInstanceInfo(db, domains);
		});
		lastActiveAccountID=prefs.getString("lastActiveAccount", null);
		maybeUpdateShortcuts();
	}

	public void addAccount(Instance instance, Token token, Account self, Application app, AccountActivationInfo activationInfo){
		instances.put(instance.getDomain(), instance);
		runOnDbThread(db->insertInstanceIntoDatabase(db, instance.getDomain(), instance, List.of(), 0));
		AccountSession session=new AccountSession(token, self, app, instance.getDomain(), activationInfo==null, activationInfo);
		sessions.put(session.getID(), session);
		lastActiveAccountID=session.getID();
		prefs.edit().putString("lastActiveAccount", lastActiveAccountID).apply();
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			session.toContentValues(values);
			db.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		});
		updateInstanceEmojis(instance, instance.getDomain());
		if(PushSubscriptionManager.arePushNotificationsAvailable()){
			session.getPushSubscriptionManager().registerAccountForPush(null);
		}
		maybeUpdateShortcuts();
	}

	@NonNull
	public List<AccountSession> getLoggedInAccounts(){
		return new ArrayList<>(sessions.values());
	}

	@NonNull
	public AccountSession getAccount(String id){
		AccountSession session=sessions.get(id);
		if(session==null)
			throw new IllegalStateException("Account session "+id+" not found");
		return session;
	}

	public static AccountSession get(String id){
		return getInstance().getAccount(id);
	}

	@Nullable
	public AccountSession tryGetAccount(String id){
		return sessions.get(id);
	}

	@Nullable
	public AccountSession getLastActiveAccount(){
		if(sessions.isEmpty() || lastActiveAccountID==null)
			return null;
		if(!sessions.containsKey(lastActiveAccountID)){
			// TODO figure out why this happens. It should not be possible.
			lastActiveAccountID=getLoggedInAccounts().get(0).getID();
			prefs.edit().putString("lastActiveAccount", lastActiveAccountID).apply();
		}
		return getAccount(lastActiveAccountID);
	}

	public String getLastActiveAccountID(){
		return lastActiveAccountID;
	}

	public void setLastActiveAccountID(String id){
		if(!sessions.containsKey(id))
			throw new IllegalStateException("Account session "+id+" not found");
		lastActiveAccountID=id;
		prefs.edit().putString("lastActiveAccount", id).apply();
	}

	public void removeAccount(String id){
		AccountSession session=getAccount(id);
		session.getCacheController().closeDatabase();
		MastodonApp.context.deleteDatabase(id+".db");
		MastodonApp.context.getSharedPreferences(id, 0).edit().clear().commit();
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			MastodonApp.context.deleteSharedPreferences(id);
		}else{
			String dataDir=MastodonApp.context.getApplicationInfo().dataDir;
			if(dataDir!=null){
				File prefsDir=new File(dataDir, "shared_prefs");
				new File(prefsDir, id+".xml").delete();
			}
		}
		sessions.remove(id);
		if(lastActiveAccountID.equals(id)){
			if(sessions.isEmpty())
				lastActiveAccountID=null;
			else
				lastActiveAccountID=getLoggedInAccounts().get(0).getID();
			prefs.edit().putString("lastActiveAccount", lastActiveAccountID).apply();
		}
		runOnDbThread(db->{
			db.delete("accounts", "`id`=?", new String[]{id});
			db.delete("instances", "`domain` NOT IN (SELECT DISTINCT `domain` FROM `accounts`)", new String[]{});
		});
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			NotificationManager nm=MastodonApp.context.getSystemService(NotificationManager.class);
			nm.deleteNotificationChannelGroup(id);
		}
		maybeUpdateShortcuts();
	}

	@NonNull
	public MastodonAPIController getUnauthenticatedApiController(){
		return unauthenticatedApiController;
	}

	public void authenticate(Activity activity, Instance instance){
		authenticatingInstance=instance;
		new CreateOAuthApp()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Application result){
						authenticatingApp=result;
						Uri uri=new Uri.Builder()
								.scheme("https")
								.authority(instance.getDomain())
								.path("/oauth/authorize")
								.appendQueryParameter("response_type", "code")
								.appendQueryParameter("client_id", result.clientId)
								.appendQueryParameter("redirect_uri", "mastodon-android-auth://callback")
								.appendQueryParameter("scope", SCOPE)
								.build();

						new CustomTabsIntent.Builder()
								.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
								.setShowTitle(true)
								.build()
								.launchUrl(activity, uri);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(activity);
					}
				})
				.wrapProgress(activity, R.string.preparing_auth, false)
				.execNoAuth(instance.getDomain());
	}

	public boolean isSelf(String id, Account other){
		return getAccount(id).self.id.equals(other.id);
	}

	public Instance getAuthenticatingInstance(){
		return authenticatingInstance;
	}

	public Application getAuthenticatingApp(){
		return authenticatingApp;
	}

	public void maybeUpdateLocalInfo(){
		long now=System.currentTimeMillis();
		HashSet<String> domains=new HashSet<>();
		for(AccountSession session:sessions.values()){
			domains.add(session.domain.toLowerCase());
			if(now-session.infoLastUpdated>24L*3600_000L){
				updateSessionLocalInfo(session);
			}
			if(!session.getLocalPreferences().serverSideFiltersSupported && now-session.filtersLastUpdated>3600_000L){
				updateSessionWordFilters(session);
			}
		}
		if(loadedInstances){
			maybeUpdateInstanceInfo(domains);
		}
	}

	private void maybeUpdateInstanceInfo(Set<String> domains){
		long now=System.currentTimeMillis();
		for(String domain:domains){
			Long lastUpdated=instancesLastUpdated.get(domain);
			if(lastUpdated==null || now-lastUpdated>24L*3600_000L){
				updateInstanceInfo(domain);
			}
		}
	}

	/*package*/ void updateSessionLocalInfo(AccountSession session){
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						session.self=result;
						session.infoLastUpdated=System.currentTimeMillis();
						runOnDbThread(db->{
							ContentValues values=new ContentValues();
							values.put("account_obj", MastodonAPIController.gson.toJson(result));
							values.put("info_last_updated", session.infoLastUpdated);
							db.update("accounts", values, "`id`=?", new String[]{session.getID()});
						});
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(session.getID());
	}

	private void updateSessionWordFilters(AccountSession session){
		new GetLegacyFilters()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<LegacyFilter> result){
						session.wordFilters=result;
						session.filtersLastUpdated=System.currentTimeMillis();
						runOnDbThread(db->{
							ContentValues values=new ContentValues();
							values.put("legacy_filters", new JsonObjectBuilder()
									.add("filters", MastodonAPIController.gson.toJsonTree(session.wordFilters))
									.add("updated", session.filtersLastUpdated)
									.build()
									.toString());
							db.update("accounts", values, "`id`=?", new String[]{session.getID()});
						});
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(session.getID());
	}

	public void updateInstanceInfo(String domain){
		loadInstanceInfo(domain, new Callback<>(){
					@Override
					public void onSuccess(Instance instance){
						instances.put(domain, instance);
						runOnDbThread(db->insertInstanceIntoDatabase(db, domain, instance, List.of(), 0));
						updateInstanceEmojis(instance, domain);
					}

					@Override
					public void onError(ErrorResponse error){

					}
				});
	}

	private void updateInstanceEmojis(Instance instance, String domain){
		new GetCustomEmojis()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Emoji> result){
						long lastUpdated=System.currentTimeMillis();
						customEmojis.put(domain, groupCustomEmojis(result));
						instancesLastUpdated.put(domain, lastUpdated);
						runOnDbThread(db->insertInstanceIntoDatabase(db, domain, instance, result, lastUpdated));
						E.post(new EmojiUpdatedEvent(domain));
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.execNoAuth(domain);
	}

	private void readInstanceInfo(SQLiteDatabase db, Set<String> domains){
		for(String domain : domains){
			final int maxEmojiLength=500000;
			try(Cursor cursor=db.rawQuery("SELECT domain, instance_obj, substr(emojis,1,?) AS emojis, length(emojis) AS emoji_length, last_updated, version FROM instances WHERE `domain` = ?",
					new String[]{String.valueOf(maxEmojiLength) , domain})) {
				ContentValues values=new ContentValues();
				while(cursor.moveToNext()){
					DatabaseUtils.cursorRowToContentValues(cursor, values);
					int version=values.getAsInteger("version");
					Instance instance=MastodonAPIController.gson.fromJson(values.getAsString("instance_obj"), switch(version){
						case 1 -> InstanceV1.class;
						case 2 -> InstanceV2.class;
						default -> throw new IllegalStateException("Unexpected value: "+version);
					});
					instances.put(domain, instance);
					StringBuilder emojiSB=new StringBuilder();
					String emojiPart=values.getAsString("emojis");
					if(TextUtils.isEmpty(emojiPart)){
						// not putting anything into instancesLastUpdated to force a reload
						continue;
					}
					emojiSB.append(emojiPart);
					//get emoji in chunks of 1MB if it didn't fit in the first query
					int emojiStringLength=values.getAsInteger("emoji_length");
					if(emojiStringLength>maxEmojiLength){
						final int pagesize=1000000;
						for(int start=maxEmojiLength + 1; start<=emojiStringLength; start+=pagesize){
							try(Cursor emojiCursor=db.rawQuery("SELECT substr(emojis,?, ?) FROM instances WHERE `domain` = ?", new String[]{String.valueOf(start), String.valueOf(pagesize), domain})){
								emojiCursor.moveToNext();
								emojiSB.append(emojiCursor.getString(0));
							}
						}
					}
					List<Emoji> emojis=MastodonAPIController.gson.fromJson(emojiSB.toString(), new TypeToken<List<Emoji>>(){}.getType());
					customEmojis.put(domain, groupCustomEmojis(emojis));
					instancesLastUpdated.put(domain, values.getAsLong("last_updated"));
				}
			}catch(Exception ex){
				Log.d(TAG, "readInstanceInfo failed", ex);
				// instancesLastUpdated will not contain that domain, so instance data will be forced to be reloaded
			}
		}
		if(!loadedInstances){
			loadedInstances=true;
			MastodonAPIController.runInBackground(()->maybeUpdateInstanceInfo(domains));
		}
	}

	private List<EmojiCategory> groupCustomEmojis(List<Emoji> emojis){
		return emojis.stream()
				.filter(e->e.visibleInPicker)
				.collect(Collectors.groupingBy(e->e.category==null ? "" : e.category))
				.entrySet()
				.stream()
				.map(e->new EmojiCategory(e.getKey(), e.getValue()))
				.sorted(Comparator.comparing(c->c.title))
				.collect(Collectors.toList());
	}

	public List<EmojiCategory> getCustomEmojis(String domain){
		List<EmojiCategory> r=customEmojis.get(domain.toLowerCase());
		return r==null ? Collections.emptyList() : r;
	}

	public Instance getInstanceInfo(String domain){
		Instance i=instances.get(domain);
		if(i!=null)
			return i;
		Log.e(TAG, "Instance info for "+domain+" was not found. This should normally never happen. Returning fake instance object");
		if(BuildConfig.DEBUG)
			throw new IllegalStateException("Instance info for "+domain+" missing");
		InstanceV1 fake=new InstanceV1();
		fake.uri=fake.title=domain;
		fake.description=fake.version=fake.email="";
		return fake;
	}

	public void updateAccountInfo(String id, Account account){
		AccountSession session=getAccount(id);
		session.self=account;
		session.infoLastUpdated=System.currentTimeMillis();
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			values.put("account_obj", MastodonAPIController.gson.toJson(account));
			values.put("info_last_updated", session.infoLastUpdated);
			db.update("accounts", values, "`id`=?", new String[]{session.getID()});
		});
	}

	public void updateAccountPreferences(String id, Preferences prefs){
		AccountSession session=getAccount(id);
		session.preferences=prefs;
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			values.put("preferences", MastodonAPIController.gson.toJson(prefs));
			db.update("accounts", values, "`id`=?", new String[]{session.getID()});
		});
	}

	public void writeAccountPushSettings(String id){
		AccountSession session=getAccount(id);
		runWithDatabase(db->{ // Called from a background thread anyway
			ContentValues values=new ContentValues();
			values.put("push_keys", new JsonObjectBuilder()
					.add("auth", session.pushAuthKey)
					.add("private", session.pushPrivateKey)
					.add("public", session.pushPublicKey)
					.build()
					.toString());
			values.put("push_subscription", MastodonAPIController.gson.toJson(session.pushSubscription));
			values.put("flags", session.getFlagsForDatabase());
			values.put("push_id", session.pushAccountID);
			db.update("accounts", values, "`id`=?", new String[]{id});
		});
	}

	public void writeAccountActivationInfo(String id){
		AccountSession session=getAccount(id);
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			values.put("activation_info", MastodonAPIController.gson.toJson(session.activationInfo));
			values.put("flags", session.getFlagsForDatabase());
			db.update("accounts", values, "`id`=?", new String[]{id});
		});
	}

	private void maybeUpdateShortcuts(){
		if(Build.VERSION.SDK_INT<26)
			return;
		ShortcutManager sm=MastodonApp.context.getSystemService(ShortcutManager.class);
		if((sm.getDynamicShortcuts().isEmpty() || BuildConfig.DEBUG) && !sessions.isEmpty()){
			// There are no shortcuts, but there are accounts. Add a compose shortcut.
			ShortcutInfo compose=new ShortcutInfo.Builder(MastodonApp.context, "compose")
					.setActivity(ComponentName.createRelative(MastodonApp.context, MainActivity.class.getName()))
					.setShortLabel(MastodonApp.context.getString(R.string.new_post))
					.setIcon(Icon.createWithResource(MastodonApp.context, R.mipmap.ic_shortcut_compose))
					.setIntent(new Intent(MastodonApp.context, MainActivity.class)
							.setAction(Intent.ACTION_MAIN)
							.putExtra("compose", true))
					.build();
			ShortcutInfo explore=new ShortcutInfo.Builder(MastodonApp.context, "explore")
					.setActivity(ComponentName.createRelative(MastodonApp.context, MainActivity.class.getName()))
					.setShortLabel(MastodonApp.context.getString(R.string.tab_search))
					.setIcon(Icon.createWithResource(MastodonApp.context, R.mipmap.ic_shortcut_explore))
					.setIntent(new Intent(MastodonApp.context, MainActivity.class)
							.setAction(Intent.ACTION_MAIN)
							.putExtra("explore", true))
					.build();
			sm.setDynamicShortcuts(List.of(compose, explore));
		}else if(sessions.isEmpty()){
			// There are shortcuts, but no accounts. Disable existing shortcuts.
			sm.disableShortcuts(List.of("compose", "explore"), MastodonApp.context.getString(R.string.err_not_logged_in));
		}else{
			sm.enableShortcuts(List.of("compose", "explore"));
		}
	}

	private void closeDelayed(){
		CacheController.databaseThread.postRunnable(databaseCloseRunnable, 10_000);
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
			CacheController.databaseThread.handler.removeCallbacks(databaseCloseRunnable);
		}
	}

	private SQLiteDatabase getOrOpenDatabase(){
		if(db==null)
			db=new DatabaseHelper();
		return db.getWritableDatabase();
	}

	private void runOnDbThread(DatabaseRunnable r){
		CacheController.databaseThread.postRunnable(()->{
			synchronized(databaseLock){
				cancelDelayedClose();
				try{
					SQLiteDatabase db=getOrOpenDatabase();
					r.run(db);
				}catch(SQLiteException|IOException x){
					Log.w(TAG, x);
				}finally{
					closeDelayed();
				}
			}
		}, 0);
	}

	private void runWithDatabase(DatabaseRunnable r){
		synchronized(databaseLock){
			cancelDelayedClose();
			try{
				SQLiteDatabase db=getOrOpenDatabase();
				r.run(db);
			}catch(SQLiteException|IOException x){
				Log.w(TAG, x);
			}finally{
				closeDelayed();
			}
		}
	}

	public void runIfDonationCampaignNotDismissed(String id, Runnable action){
		runOnDbThread(db->{
			try(Cursor cursor=db.query("dismissed_donation_campaigns", null, "id=?", new String[]{id}, null, null, null)){
				if(!cursor.moveToFirst()){
					UiUtils.runOnUiThread(action);
				}
			}
		});
	}

	public void markDonationCampaignAsDismissed(String id){
		runOnDbThread(db->{
			ContentValues values=new ContentValues();
			values.put("id", id);
			values.put("dismissed_at", System.currentTimeMillis());
			db.insert("dismissed_donation_campaigns", null, values);
		});
	}

	public void clearDismissedDonationCampaigns(){
		runOnDbThread(db->db.delete("dismissed_donation_campaigns", null, null));
	}

	public void clearInstanceInfo(){
		SQLiteDatabase db=getOrOpenDatabase();
		db.delete("instances", null, null);
		db.close();
	}

	private static void insertInstanceIntoDatabase(SQLiteDatabase db, String domain, Instance instance, List<Emoji> emojis, long lastUpdated){
		ContentValues values=new ContentValues();
		values.put("domain", domain);
		values.put("instance_obj", MastodonAPIController.gson.toJson(instance));
		values.put("emojis", MastodonAPIController.gson.toJson(emojis));
		values.put("last_updated", lastUpdated);
		values.put("version", instance.getVersion());
		db.insertWithOnConflict("instances", null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public static APIRequest<Instance> loadInstanceInfo(String domain, Callback<Instance> callback){
		final WrapperRequest<Instance> wrapper=new WrapperRequest<>();
		wrapper.wrappedRequest=new GetInstanceV2()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(InstanceV2 result){
						wrapper.wrappedRequest=null;
						callback.onSuccess(result);
					}

					@Override
					public void onError(ErrorResponse error){
						if(error instanceof MastodonErrorResponse mr && mr.httpStatus==404){
							// Mastodon pre-4.0 or a non-Mastodon server altogether. Let's try /api/v1/instance
							wrapper.wrappedRequest=new GetInstanceV1()
									.setCallback(new Callback<>(){
										@Override
										public void onSuccess(InstanceV1 result){
											wrapper.wrappedRequest=null;
											callback.onSuccess(result);
										}

										@Override
										public void onError(ErrorResponse error){
											wrapper.wrappedRequest=null;
											callback.onError(error);
										}
									})
									.execNoAuth(domain);
						}else{
							wrapper.wrappedRequest=null;
							callback.onError(error);
						}
					}
				})
				.execNoAuth(domain);
		return wrapper;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper{
		public DatabaseHelper(){
			super(MastodonApp.context, "accounts.db", null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db){
			db.execSQL("""
						CREATE TABLE `dismissed_donation_campaigns` (
							`id` text PRIMARY KEY,
							`dismissed_at` bigint
						)""");
			createAccountsTable(db);
			db.execSQL("""
						CREATE TABLE `instances` (
							`domain` text PRIMARY KEY,
							`instance_obj` text,
							`emojis` text,
							`last_updated` bigint,
							`version` integer NOT NULL DEFAULT 1
						)""");
			maybeMigrateAccounts(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			if(oldVersion<2){
				createAccountsTable(db);
				db.execSQL("""
						CREATE TABLE `instances` (
							`domain` text PRIMARY KEY,
							`instance_obj` text,
							`emojis` text,
							`last_updated` bigint
						)""");
				maybeMigrateAccounts(db);
			}
			if(oldVersion<3){
				db.execSQL("ALTER TABLE `instances` ADD `version` integer NOT NULL DEFAULT 1");
			}
		}

		private void createAccountsTable(SQLiteDatabase db){
			db.execSQL("""
						CREATE TABLE `accounts` (
							`id` text PRIMARY KEY,
							`domain` text,
							`account_obj` text,
							`token` text,
							`application` text,
							`info_last_updated` bigint,
							`flags` bigint,
							`push_keys` text,
							`push_subscription` text,
							`legacy_filters` text DEFAULT NULL,
							`push_id` text,
							`activation_info` text,
							`preferences` text
						)""");
		}

		private void maybeMigrateAccounts(SQLiteDatabase db){
			File accountsFile=new File(MastodonApp.context.getFilesDir(), "accounts.json");
			if(accountsFile.exists()){
				HashSet<String> domains=new HashSet<>();
				try(FileInputStream in=new FileInputStream(accountsFile)){
					JsonObject jobj=JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
					ContentValues values=new ContentValues();
					JsonArray accounts=jobj.has("a") ? jobj.getAsJsonArray("a") : jobj.getAsJsonArray("accounts");
					for(JsonElement jacc:accounts){
						AccountSession session=MastodonAPIController.gson.fromJson(jacc, AccountSession.class);
						domains.add(session.domain.toLowerCase());
						session.toContentValues(values);
						db.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
					}
				}catch(Exception x){
					Log.e(TAG, "Error migrating accounts", x);
					return;
				}
				accountsFile.delete();
				for(String domain:domains){
					File file=new File(MastodonApp.context.getFilesDir(), "instance_"+domain.replace('.', '_')+".json");
					try(FileInputStream in=new FileInputStream(file)){
						JsonObject jobj=JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
						insertInstanceIntoDatabase(db, domain, MastodonAPIController.gson.fromJson(jobj.get(jobj.has("instance") ? "instance" : "a"), Instance.class),
								MastodonAPIController.gson.fromJson(jobj.get("emojis"), new TypeToken<>(){}.getType()), jobj.get("last_updated").getAsLong());
					}catch(Exception x){
						Log.w(TAG, "Error reading instance info file for "+domain, x);
					}
					file.delete();
				}
			}
		}
	}
}
