package org.joinmastodon.android.api.session;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonParseException;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.GetCustomEmojis;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.oauth.CreateOAuthApp;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Token;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountSessionManager{
	private static final String TAG="AccountSessionManager";
	public static final String SCOPE="read write follow push";
	public static final String REDIRECT_URI="mastodon-android-auth://callback";

	private static final AccountSessionManager instance=new AccountSessionManager();

	private HashMap<String, AccountSession> sessions=new HashMap<>();
	private HashMap<String, List<EmojiCategory>> customEmojis=new HashMap<>();
	private HashMap<String, Long> customEmojisLastUpdated=new HashMap<>();
	private MastodonAPIController unauthenticatedApiController=new MastodonAPIController(null);
	private Instance authenticatingInstance;
	private Application authenticatingApp;
	private String lastActiveAccountID;
	private SharedPreferences prefs;
	private boolean loadedCustomEmojis;

	public static AccountSessionManager getInstance(){
		return instance;
	}

	private AccountSessionManager(){
		prefs=MastodonApp.context.getSharedPreferences("account_manager", Context.MODE_PRIVATE);
		File file=new File(MastodonApp.context.getFilesDir(), "accounts.json");
		if(!file.exists())
			return;
		HashSet<String> domains=new HashSet<>();
		try(FileInputStream in=new FileInputStream(file)){
			SessionsStorageWrapper w=MastodonAPIController.gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), SessionsStorageWrapper.class);
			for(AccountSession session:w.accounts){
				domains.add(session.domain.toLowerCase());
				sessions.put(session.getID(), session);
			}
		}catch(IOException|JsonParseException x){
			Log.e(TAG, "Error loading accounts", x);
		}
		lastActiveAccountID=prefs.getString("lastActiveAccount", null);
		MastodonAPIController.runInBackground(()->readCustomEmojis(domains));
	}

	public void addAccount(Instance instance, Token token, Account self, Application app){
		AccountSession session=new AccountSession(token, self, app, instance.uri, instance.maxTootChars);
		sessions.put(session.getID(), session);
		lastActiveAccountID=session.getID();
		writeAccountsFile();
	}

	private void writeAccountsFile(){
		File file=new File(MastodonApp.context.getFilesDir(), "accounts.json");
		try{
			try(FileOutputStream out=new FileOutputStream(file)){
				SessionsStorageWrapper w=new SessionsStorageWrapper();
				w.accounts=new ArrayList<>(sessions.values());
				OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
				MastodonAPIController.gson.toJson(w, writer);
				writer.flush();
			}
		}catch(IOException x){
			Log.e(TAG, "Error writing accounts file", x);
		}
		prefs.edit().putString("lastActiveAccount", lastActiveAccountID).apply();
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

	@Nullable
	public AccountSession getLastActiveAccount(){
		if(sessions.isEmpty() || lastActiveAccountID==null)
			return null;
		return getAccount(lastActiveAccountID);
	}

	public String getLastActiveAccountID(){
		return lastActiveAccountID;
	}

	public void removeAccount(String id){
		AccountSession session=getAccount(id);
		sessions.remove(id);
		if(lastActiveAccountID.equals(id)){
			if(sessions.isEmpty())
				lastActiveAccountID=null;
			else
				lastActiveAccountID=getLoggedInAccounts().get(0).getID();
		}
		writeAccountsFile();
		String domain=session.domain.toLowerCase();
		if(sessions.isEmpty() || !sessions.values().stream().map(s->s.domain.toLowerCase()).collect(Collectors.toSet()).contains(domain)){
			getCustomEmojisFile(domain).delete();
		}
	}

	@NonNull
	public MastodonAPIController getUnauthenticatedApiController(){
		return unauthenticatedApiController;
	}

	public void authenticate(Context context, Instance instance){
		authenticatingInstance=instance;
		ProgressDialog progress=new ProgressDialog(context);
		progress.setMessage(context.getString(R.string.preparing_auth));
		progress.setCancelable(false);
		progress.show();
		new CreateOAuthApp()
				.setCallback(new Callback<Application>(){
					@Override
					public void onSuccess(Application result){
						authenticatingApp=result;
						progress.dismiss();
						Uri uri=new Uri.Builder()
								.scheme("https")
								.authority(instance.uri)
								.path("/oauth/authorize")
								.appendQueryParameter("response_type", "code")
								.appendQueryParameter("client_id", result.clientId)
								.appendQueryParameter("redirect_uri", "mastodon-android-auth://callback")
								.appendQueryParameter("scope", "read write follow push")
								.build();

						new CustomTabsIntent.Builder()
								.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
								.build()
								.launchUrl(context, uri);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(context);
						progress.dismiss();
					}
				})
				.execNoAuth(instance.uri);
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
		}
		if(loadedCustomEmojis){
			maybeUpdateCustomEmojis(domains);
		}
	}

	private void maybeUpdateCustomEmojis(Set<String> domains){
		long now=System.currentTimeMillis();
		for(String domain:domains){
			Long lastUpdated=customEmojisLastUpdated.get(domain);
			if(lastUpdated==null || now-lastUpdated>24L*3600_000L){
				updateCustomEmojis(domain);
			}
		}
	}

	private void updateSessionLocalInfo(AccountSession session){
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						session.self=result;
						session.infoLastUpdated=System.currentTimeMillis();
						writeAccountsFile();
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(session.getID());
	}

	private void updateCustomEmojis(String domain){
		new GetCustomEmojis()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Emoji> result){
						CustomEmojisStorageWrapper emojis=new CustomEmojisStorageWrapper();
						emojis.lastUpdated=System.currentTimeMillis();
						emojis.emojis=result;
						customEmojis.put(domain, groupCustomEmojis(emojis));
						customEmojisLastUpdated.put(domain, emojis.lastUpdated);
						MastodonAPIController.runInBackground(()->writeCustomEmojisFile(emojis, domain));
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.execNoAuth(domain);
	}

	private File getCustomEmojisFile(String domain){
		return new File(MastodonApp.context.getFilesDir(), "emojis_"+domain.replace('.', '_')+".json");
	}

	private void writeCustomEmojisFile(CustomEmojisStorageWrapper emojis, String domain){
		try(FileOutputStream out=new FileOutputStream(getCustomEmojisFile(domain))){
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			MastodonAPIController.gson.toJson(emojis, writer);
			writer.flush();
		}catch(IOException x){
			Log.w(TAG, "Error writing emojis file for "+domain, x);
		}
	}

	private void readCustomEmojis(Set<String> domains){
		for(String domain:domains){
			try(FileInputStream in=new FileInputStream(getCustomEmojisFile(domain))){
				InputStreamReader reader=new InputStreamReader(in, StandardCharsets.UTF_8);
				CustomEmojisStorageWrapper emojis=MastodonAPIController.gson.fromJson(reader, CustomEmojisStorageWrapper.class);
				customEmojis.put(domain, groupCustomEmojis(emojis));
				customEmojisLastUpdated.put(domain, emojis.lastUpdated);
			}catch(IOException|JsonParseException x){
				Log.w(TAG, "Error reading emojis file for "+domain, x);
			}
		}
		if(!loadedCustomEmojis){
			loadedCustomEmojis=true;
			maybeUpdateCustomEmojis(domains);
		}
	}

	private List<EmojiCategory> groupCustomEmojis(CustomEmojisStorageWrapper emojis){
		return emojis.emojis.stream()
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

	private static class SessionsStorageWrapper{
		public List<AccountSession> accounts;
	}

	private static class CustomEmojisStorageWrapper{
		public List<Emoji> emojis;
		public long lastUpdated;
	}
}
