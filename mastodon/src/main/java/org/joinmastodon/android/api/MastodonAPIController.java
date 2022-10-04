package org.joinmastodon.android.api;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.api.gson.IsoInstantTypeAdapter;
import org.joinmastodon.android.api.gson.IsoLocalDateTypeAdapter;
import org.joinmastodon.android.api.session.AccountSession;

import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.WorkerThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MastodonAPIController{
	private static final String TAG="MastodonAPIController";
	public static final Gson gson=new GsonBuilder()
			.disableHtmlEscaping()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.registerTypeAdapter(Instant.class, new IsoInstantTypeAdapter())
			.registerTypeAdapter(LocalDate.class, new IsoLocalDateTypeAdapter())
			.create();
	private static WorkerThread thread=new WorkerThread("MastodonAPIController");
	private static OkHttpClient httpClient=new OkHttpClient.Builder().build();

	private AccountSession session;

	static{
		thread.start();
	}

	public MastodonAPIController(@Nullable AccountSession session){
		this.session=session;
	}

	public <T> void submitRequest(final MastodonAPIRequest<T> req){
		thread.postRunnable(()->{
			try{
				if(req.canceled)
					return;
				Request.Builder builder=new Request.Builder()
						.url(req.getURL().toString())
						.method(req.getMethod(), req.getRequestBody())
						.header("User-Agent", "MastodonAndroid/"+BuildConfig.VERSION_NAME);

				String token=null;
				if(session!=null)
					token=session.token.accessToken;
				else if(req.token!=null)
					token=req.token.accessToken;

				if(token!=null)
					builder.header("Authorization", "Bearer "+token);

				if(req.headers!=null){
					for(Map.Entry<String, String> header:req.headers.entrySet()){
						builder.header(header.getKey(), header.getValue());
					}
				}

				Request hreq=builder.build();
				Call call=httpClient.newCall(hreq);
				synchronized(req){
					req.okhttpCall=call;
				}

				if(BuildConfig.DEBUG)
					Log.d(TAG, "["+(session==null ? "no-auth" : session.getID())+"] Sending request: "+hreq);

				call.enqueue(new Callback(){
					@Override
					public void onFailure(@NonNull Call call, @NonNull IOException e){
						if(call.isCanceled())
							return;
						if(BuildConfig.DEBUG)
							Log.w(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+hreq+" failed", e);
						synchronized(req){
							req.okhttpCall=null;
						}
						req.onError(e.getLocalizedMessage(), 0, e);
					}

					@Override
					public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
						if(call.isCanceled())
							return;
						if(BuildConfig.DEBUG)
							Log.d(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+hreq+" received response: "+response);
						synchronized(req){
							req.okhttpCall=null;
						}
						try(ResponseBody body=response.body()){
							Reader reader=body.charStream();
							if(response.isSuccessful()){
								T respObj;
								try{
									if(BuildConfig.DEBUG){
										JsonElement respJson=JsonParser.parseReader(reader);
										Log.d(TAG, "["+(session==null ? "no-auth" : session.getID())+"] response body: "+respJson);
										if(req.respTypeToken!=null)
											respObj=gson.fromJson(respJson, req.respTypeToken.getType());
										else
											respObj=gson.fromJson(respJson, req.respClass);
									}else{
										if(req.respTypeToken!=null)
											respObj=gson.fromJson(reader, req.respTypeToken.getType());
										else
											respObj=gson.fromJson(reader, req.respClass);
									}
								}catch(JsonIOException|JsonSyntaxException x){
									if(BuildConfig.DEBUG)
										Log.w(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+response+" error parsing or reading body", x);
									req.onError(x.getLocalizedMessage(), response.code(), x);
									return;
								}

								try{
									req.validateAndPostprocessResponse(respObj, response);
								}catch(IOException x){
									if(BuildConfig.DEBUG)
										Log.w(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+response+" error post-processing or validating response", x);
									req.onError(x.getLocalizedMessage(), response.code(), x);
									return;
								}

								if(BuildConfig.DEBUG)
									Log.d(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+response+" parsed successfully: "+respObj);

								req.onSuccess(respObj);
							}else{
								try{
									JsonObject error=JsonParser.parseReader(reader).getAsJsonObject();
									Log.w(TAG, "["+(session==null ? "no-auth" : session.getID())+"] "+response+" received error: "+error);
									if(error.has("details")){
										MastodonDetailedErrorResponse err=new MastodonDetailedErrorResponse(error.get("error").getAsString(), response.code(), null);
										HashMap<String, List<MastodonDetailedErrorResponse.FieldError>> details=new HashMap<>();
										JsonObject errorDetails=error.getAsJsonObject("details");
										for(String key:errorDetails.keySet()){
											ArrayList<MastodonDetailedErrorResponse.FieldError> fieldErrors=new ArrayList<>();
											for(JsonElement el:errorDetails.getAsJsonArray(key)){
												JsonObject eobj=el.getAsJsonObject();
												MastodonDetailedErrorResponse.FieldError fe=new MastodonDetailedErrorResponse.FieldError();
												fe.description=eobj.get("description").getAsString();
												fe.error=eobj.get("error").getAsString();
												fieldErrors.add(fe);
											}
											details.put(key, fieldErrors);
										}
										err.detailedErrors=details;
										req.onError(err);
									}else{
										req.onError(error.get("error").getAsString(), response.code(), null);
									}
								}catch(JsonIOException|JsonSyntaxException x){
									req.onError(response.code()+" "+response.message(), response.code(), x);
								}catch(Exception x){
									req.onError("Error parsing an API error", response.code(), x);
								}
							}
						}catch(Exception x){
							Log.w(TAG, "onResponse: error processing response", x);
							onFailure(call, (IOException) new IOException(x).fillInStackTrace());
						}
					}
				});
			}catch(Exception x){
				if(BuildConfig.DEBUG)
					Log.w(TAG, "["+(session==null ? "no-auth" : session.getID())+"] error creating and sending http request", x);
				req.onError(x.getLocalizedMessage(), 0, x);
			}
		}, 0);
	}

	public static void runInBackground(Runnable action){
		thread.postRunnable(action, 0);
	}
}
