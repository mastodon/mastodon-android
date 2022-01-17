package org.joinmastodon.android.api.session;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.OAuthActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.oauth.CreateOAuthApp;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
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
import java.util.HashMap;
import java.util.List;
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
	private MastodonAPIController unauthenticatedApiController=new MastodonAPIController(null);
	private Instance authenticatingInstance;
	private Application authenticatingApp;
	private String lastActiveAccountID;
	private SharedPreferences prefs;

	public static AccountSessionManager getInstance(){
		return instance;
	}

	private AccountSessionManager(){
		prefs=MastodonApp.context.getSharedPreferences("account_manager", Context.MODE_PRIVATE);
		File file=new File(MastodonApp.context.getFilesDir(), "accounts.json");
		if(!file.exists())
			return;
		try(FileInputStream in=new FileInputStream(file)){
			SessionsStorageWrapper w=MastodonAPIController.gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), SessionsStorageWrapper.class);
			for(AccountSession session:w.accounts){
				sessions.put(session.getID(), session);
			}
		}catch(IOException x){
			Log.e(TAG, "Error loading accounts", x);
		}
		lastActiveAccountID=prefs.getString("lastActiveAccount", null);
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

	public Instance getAuthenticatingInstance(){
		return authenticatingInstance;
	}

	public Application getAuthenticatingApp(){
		return authenticatingApp;
	}

	private static class SessionsStorageWrapper{
		public List<AccountSession> accounts;
	}
}
