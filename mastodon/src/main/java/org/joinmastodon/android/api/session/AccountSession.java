package org.joinmastodon.android.api.session;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.api.gson.JsonObjectBuilder;
import org.joinmastodon.android.api.requests.accounts.GetPreferences;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentialsPreferences;
import org.joinmastodon.android.api.requests.markers.GetMarkers;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineMarkers;
import org.joinmastodon.android.model.Token;
import org.joinmastodon.android.utils.ObjectIdComparator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountSession{
	private static final String TAG="AccountSession";
	private static final int MIN_DAYS_ACCOUNT_AGE_FOR_DONATIONS=28;

	public static final int FLAG_ACTIVATED=1;
	public static final int FLAG_NEED_UPDATE_PUSH_SETTINGS=1 << 1;
	public static final int FLAG_NEED_RE_REGISTER_PUSH=1 << 2;

	@SerializedName(value="token", alternate="a")
	public Token token;
	@SerializedName(value="self", alternate="b")
	public Account self;
	@SerializedName(value="domain", alternate="c")
	public String domain;
	@SerializedName(value="app", alternate="d")
	public Application app;
	@SerializedName(value="info_last_updated", alternate="e")
	public long infoLastUpdated;
	@SerializedName(value="activated", alternate="f")
	public boolean activated=true;
	@SerializedName(value="push_private_key", alternate="g")
	public String pushPrivateKey;
	@SerializedName(value="push_public_key", alternate="h")
	public String pushPublicKey;
	@SerializedName(value="push_auth_key", alternate="i")
	public String pushAuthKey;
	@SerializedName(value="push_subscription", alternate="j")
	public PushSubscription pushSubscription;
	@SerializedName(value="need_update_push_settings", alternate="k")
	public boolean needUpdatePushSettings;
	@SerializedName(value="filters_last_updated", alternate="l")
	public long filtersLastUpdated;
	@SerializedName(value="word_filters", alternate="m")
	public List<LegacyFilter> wordFilters=new ArrayList<>();
	@SerializedName(value="push_account_i_d", alternate="n")
	public String pushAccountID;
	@SerializedName(value="activation_info", alternate="o")
	public AccountActivationInfo activationInfo;
	@SerializedName(value="preferences", alternate="p")
	public Preferences preferences;
	public boolean needReRegisterForPush;
	private transient MastodonAPIController apiController;
	private transient StatusInteractionController statusInteractionController;
	private transient CacheController cacheController;
	private transient PushSubscriptionManager pushSubscriptionManager;
	private transient SharedPreferences prefs;
	private transient boolean preferencesNeedSaving;
	private transient AccountLocalPreferences localPreferences;

	AccountSession(Token token, Account self, Application app, String domain, boolean activated, AccountActivationInfo activationInfo){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.activated=activated;
		this.activationInfo=activationInfo;
		infoLastUpdated=System.currentTimeMillis();
	}

	AccountSession(){}

	AccountSession(ContentValues values){
		domain=values.getAsString("domain");
		self=MastodonAPIController.gson.fromJson(values.getAsString("account_obj"), Account.class);
		token=MastodonAPIController.gson.fromJson(values.getAsString("token"), Token.class);
		app=MastodonAPIController.gson.fromJson(values.getAsString("application"), Application.class);
		infoLastUpdated=values.getAsLong("info_last_updated");
		long flags=values.getAsLong("flags");
		activated=(flags & FLAG_ACTIVATED)==FLAG_ACTIVATED;
		needUpdatePushSettings=(flags & FLAG_NEED_UPDATE_PUSH_SETTINGS)==FLAG_NEED_UPDATE_PUSH_SETTINGS;
		needReRegisterForPush=(flags & FLAG_NEED_RE_REGISTER_PUSH)==FLAG_NEED_RE_REGISTER_PUSH;
		JsonObject pushKeys=JsonParser.parseString(values.getAsString("push_keys")).getAsJsonObject();
		if(!pushKeys.get("auth").isJsonNull() && !pushKeys.get("private").isJsonNull() && !pushKeys.get("public").isJsonNull()){
			pushAuthKey=pushKeys.get("auth").getAsString();
			pushPrivateKey=pushKeys.get("private").getAsString();
			pushPublicKey=pushKeys.get("public").getAsString();
		}
		pushSubscription=MastodonAPIController.gson.fromJson(values.getAsString("push_subscription"), PushSubscription.class);
		JsonObject legacyFilters=JsonParser.parseString(values.getAsString("legacy_filters")).getAsJsonObject();
		wordFilters=MastodonAPIController.gson.fromJson(legacyFilters.getAsJsonArray("filters"), new TypeToken<List<LegacyFilter>>(){}.getType());
		filtersLastUpdated=legacyFilters.get("updated").getAsLong();
		pushAccountID=values.getAsString("push_id");
		activationInfo=MastodonAPIController.gson.fromJson(values.getAsString("activation_info"), AccountActivationInfo.class);
		preferences=MastodonAPIController.gson.fromJson(values.getAsString("preferences"), Preferences.class);
	}

	public void toContentValues(ContentValues values){
		values.put("id", getID());
		values.put("domain", domain.toLowerCase());
		values.put("account_obj", MastodonAPIController.gson.toJson(self));
		values.put("token", MastodonAPIController.gson.toJson(token));
		values.put("application", MastodonAPIController.gson.toJson(app));
		values.put("info_last_updated", infoLastUpdated);
		values.put("flags", getFlagsForDatabase());
		values.put("push_keys", new JsonObjectBuilder()
				.add("auth", pushAuthKey)
				.add("private", pushPrivateKey)
				.add("public", pushPublicKey)
				.build()
				.toString());
		values.put("push_subscription", MastodonAPIController.gson.toJson(pushSubscription));
		values.put("legacy_filters", new JsonObjectBuilder()
				.add("filters", MastodonAPIController.gson.toJsonTree(wordFilters))
				.add("updated", filtersLastUpdated)
				.build()
				.toString());
		values.put("push_id", pushAccountID);
		values.put("activation_info", MastodonAPIController.gson.toJson(activationInfo));
		values.put("preferences", MastodonAPIController.gson.toJson(preferences));
	}

	public long getFlagsForDatabase(){
		long flags=0;
		if(activated)
			flags|=FLAG_ACTIVATED;
		if(needUpdatePushSettings)
			flags|=FLAG_NEED_UPDATE_PUSH_SETTINGS;
		if(needReRegisterForPush)
			flags|=FLAG_NEED_RE_REGISTER_PUSH;
		return flags;
	}

	public String getID(){
		return domain+"_"+self.id;
	}

	public MastodonAPIController getApiController(){
		if(apiController==null)
			apiController=new MastodonAPIController(this);
		return apiController;
	}

	public StatusInteractionController getStatusInteractionController(){
		if(statusInteractionController==null)
			statusInteractionController=new StatusInteractionController(getID());
		return statusInteractionController;
	}

	public CacheController getCacheController(){
		if(cacheController==null)
			cacheController=new CacheController(getID());
		return cacheController;
	}

	public PushSubscriptionManager getPushSubscriptionManager(){
		if(pushSubscriptionManager==null)
			pushSubscriptionManager=new PushSubscriptionManager(getID());
		return pushSubscriptionManager;
	}

	public String getFullUsername(){
		return '@'+self.username+'@'+domain;
	}

	public void reloadPreferences(Consumer<Preferences> callback){
		new GetPreferences()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Preferences result){
						preferences=result;
						if(callback!=null)
							callback.accept(result);
						AccountSessionManager.getInstance().updateAccountPreferences(getID(), result);
					}

					@Override
					public void onError(ErrorResponse error){
						Log.w(TAG, "Failed to load preferences for account "+getID()+": "+error);
					}
				})
				.exec(getID());
	}

	public SharedPreferences getRawLocalPreferences(){
		if(prefs==null)
			prefs=MastodonApp.context.getSharedPreferences(getID(), Context.MODE_PRIVATE);
		return prefs;
	}

	public void reloadNotificationsMarker(Consumer<String> callback){
		new GetMarkers()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(TimelineMarkers result){
						if(result.notifications!=null && !TextUtils.isEmpty(result.notifications.lastReadId)){
							String id=result.notifications.lastReadId;
							String lastKnown=getLastKnownNotificationsMarker();
							if(ObjectIdComparator.INSTANCE.compare(id, lastKnown)<0){
								// Marker moved back -- previous marker update must have failed.
								// Pretend it didn't happen and repeat the request.
								id=lastKnown;
								new SaveMarkers(null, id).exec(getID());
							}
							callback.accept(id);
							setNotificationsMarker(id, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){}
				})
				.exec(getID());
	}

	public String getLastKnownNotificationsMarker(){
		return getRawLocalPreferences().getString("notificationsMarker", null);
	}

	public void setNotificationsMarker(String id, boolean clearUnread){
		getRawLocalPreferences().edit().putString("notificationsMarker", id).apply();
		E.post(new NotificationsMarkerUpdatedEvent(getID(), id, clearUnread));
	}

	public void logOut(Activity activity, Runnable onDone){
		new RevokeOauthToken(app.clientId, app.clientSecret, token.accessToken)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						AccountSessionManager.getInstance().removeAccount(getID());
						onDone.run();
					}

					@Override
					public void onError(ErrorResponse error){
						AccountSessionManager.getInstance().removeAccount(getID());
						onDone.run();
					}
				})
				.wrapProgress(activity, R.string.loading, false)
				.exec(getID());
	}

	public void savePreferencesLater(){
		preferencesNeedSaving=true;
	}

	public void savePreferencesIfPending(){
		if(preferencesNeedSaving){
			new UpdateAccountCredentialsPreferences(preferences, null, self.discoverable, self.source.indexable)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Account result){
							preferencesNeedSaving=false;
							self=result;
							AccountSessionManager.getInstance().updateAccountInfo(getID(), self);
						}

						@Override
						public void onError(ErrorResponse error){
							Log.e(TAG, "failed to save preferences: "+error);
						}
					})
					.exec(getID());
		}
	}

	public AccountLocalPreferences getLocalPreferences(){
		if(localPreferences==null)
			localPreferences=new AccountLocalPreferences(getRawLocalPreferences());
		return localPreferences;
	}

	public void filterStatuses(List<Status> statuses, FilterContext context){
		filterStatusContainingObjects(statuses, Function.identity(), context);
	}

	public <T> void filterStatusContainingObjects(List<T> objects, Function<T, Status> extractor, FilterContext context){
		if(getLocalPreferences().serverSideFiltersSupported){
			// Even with server-side filters, clients are expected to remove statuses that match a filter that hides them
			objects.removeIf(o->{
				Status s=extractor.apply(o);
				if(s==null)
					return false;
				if(s.filtered==null)
					return false;
				for(FilterResult filter:s.filtered){
					if(filter.filter.isActive() && filter.filter.filterAction==FilterAction.HIDE)
						return true;
				}
				return false;
			});
			return;
		}
		if(wordFilters==null)
			return;
		for(T obj:objects){
			Status s=extractor.apply(obj);
			if(s!=null && s.filtered!=null){
				getLocalPreferences().serverSideFiltersSupported=true;
				getLocalPreferences().save();
				return;
			}
		}
		objects.removeIf(o->{
			Status s=extractor.apply(o);
			if(s==null)
				return false;
			for(LegacyFilter filter:wordFilters){
				if(filter.context.contains(context) && filter.matches(s) && filter.isActive())
					return true;
			}
			return false;
		});
	}

	public void updateAccountInfo(){
		AccountSessionManager.getInstance().updateSessionLocalInfo(this);
	}

	public boolean isNotificationsMentionsOnly(){
		return getRawLocalPreferences().getBoolean("notificationsMentionsOnly", false);
	}

	public void setNotificationsMentionsOnly(boolean mentionsOnly){
		getRawLocalPreferences().edit().putBoolean("notificationsMentionsOnly", mentionsOnly).apply();
	}

	public boolean isEligibleForDonations(){
		return ("mastodon.social".equalsIgnoreCase(domain) || "mastodon.online".equalsIgnoreCase(domain)) && self.createdAt.isBefore(Instant.now().minus(MIN_DAYS_ACCOUNT_AGE_FOR_DONATIONS, ChronoUnit.DAYS));
	}

	public int getDonationSeed(){
		return Math.abs(getFullUsername().hashCode())%100;
	}

	public Instance getInstanceInfo(){
		return AccountSessionManager.getInstance().getInstanceInfo(domain);
	}

	public boolean hasPushCredentials(){
		return pushAccountID!=null && pushAuthKey!=null && pushPrivateKey!=null && pushPublicKey!=null;
	}
}
