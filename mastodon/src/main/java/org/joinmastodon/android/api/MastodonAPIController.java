package org.joinmastodon.android.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.gson.IsoInstantTypeAdapter;
import org.joinmastodon.android.api.gson.IsoLocalDateTypeAdapter;
import org.joinmastodon.android.api.requests.async_refreshes.GetAsyncRefresh;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.model.AsyncRefresh;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.WorkerThread;
import okhttp3.Cache;
import okhttp3.CacheControl;
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
	private static OkHttpClient httpClient=new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.cache(new Cache(new File(MastodonApp.context.getCacheDir(), "http"), 10*1024*1024))
			.build();
	private static Handler uiThreadHandler=new Handler(Looper.getMainLooper());

	private static final CacheControl NO_CACHE_WHATSOEVER=new CacheControl.Builder().noCache().noStore().build();

	private AccountSession session;
	private HashMap<String, AsyncRefreshPollRecord> asyncRefreshPolls=new HashMap<>();

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

				if(!req.cacheable)
					builder.cacheControl(NO_CACHE_WHATSOEVER);

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
				if(req.timeout>0){
					call.timeout().timeout(req.timeout, TimeUnit.MILLISECONDS);
				}

				if(BuildConfig.DEBUG)
					Log.d(TAG, logTag(session)+"Sending request: "+hreq);

				call.enqueue(new Callback(){
					@Override
					public void onFailure(@NonNull Call call, @NonNull IOException e){
						if(req.canceled)
							return;
						if(BuildConfig.DEBUG)
							Log.w(TAG, logTag(session)+""+hreq+" failed", e);
						synchronized(req){
							req.okhttpCall=null;
						}
						req.onError(e.getLocalizedMessage(), 0, e);
					}

					@Override
					public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
						if(req.canceled){
							response.close();
							return;
						}
						if(BuildConfig.DEBUG)
							Log.d(TAG, logTag(session)+hreq+" received response: "+response);
						synchronized(req){
							req.okhttpCall=null;
						}
						if(BuildConfig.DEBUG){
							String deprecationHeader=response.header("Deprecation");
							if(deprecationHeader!=null && deprecationHeader.startsWith("@")){
								try{
									Instant date=Instant.ofEpochSecond(Long.parseLong(deprecationHeader.substring(1)));
									String msg=hreq.url().encodedPath();
									if(date.isAfter(Instant.now()))
										msg+=" will be deprecated on ";
									else
										msg+=" is deprecated as of ";
									msg+=date.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
									Log.w(TAG, logTag(session)+msg);
									final String finalMsg=msg;
									uiThreadHandler.post(()->Toast.makeText(MastodonApp.context, finalMsg, Toast.LENGTH_SHORT).show());
								}catch(NumberFormatException ignored){}
							}
						}
						try(ResponseBody body=response.body()){
							Reader reader=body.charStream();
							if(response.isSuccessful()){
								T respObj;
								try{
									if(BuildConfig.DEBUG){
										JsonElement respJson=JsonParser.parseReader(reader);
										Log.d(TAG, logTag(session)+"response body: "+respJson);
										if(req.respTypeToken!=null)
											respObj=gson.fromJson(respJson, req.respTypeToken.getType());
										else if(req.respClass!=null)
											respObj=gson.fromJson(respJson, req.respClass);
										else
											respObj=null;
									}else{
										if(req.respTypeToken!=null)
											respObj=gson.fromJson(reader, req.respTypeToken.getType());
										else if(req.respClass!=null)
											respObj=gson.fromJson(reader, req.respClass);
										else
											respObj=null;
									}
								}catch(JsonIOException|JsonSyntaxException x){
									if(BuildConfig.DEBUG)
										Log.w(TAG, logTag(session)+response+" error parsing or reading body", x);
									req.onError(x.getLocalizedMessage(), response.code(), x);
									return;
								}

								try{
									req.validateAndPostprocessResponse(respObj, response);
								}catch(IOException x){
									if(BuildConfig.DEBUG)
										Log.w(TAG, logTag(session)+response+" error post-processing or validating response", x);
									req.onError(x.getLocalizedMessage(), response.code(), x);
									return;
								}

								if(BuildConfig.DEBUG)
									Log.d(TAG, logTag(session)+response+" parsed successfully: "+respObj);

								req.onSuccess(respObj);
							}else{
								try{
									JsonObject error=JsonParser.parseReader(reader).getAsJsonObject();
									Log.w(TAG, logTag(session)+response+" received error: "+error);
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
					Log.w(TAG, logTag(session)+"error creating and sending http request", x);
				req.onError(x.getLocalizedMessage(), 0, x);
			}
		}, 0);
	}

	public static void runInBackground(Runnable action){
		thread.postRunnable(action, 0);
	}

	public static OkHttpClient getHttpClient(){
		return httpClient;
	}

	private static String logTag(AccountSession session){
		return "["+(session==null ? "no-auth" : session.getID())+"] ";
	}

	public void startPollingAsyncRefresh(AsyncRefreshHeader header, Consumer<AsyncRefresh> callback){
		AsyncRefreshPollRecord existingRecord=asyncRefreshPolls.get(header.id);
		if(existingRecord!=null){
			if(existingRecord.callbacks.contains(callback))
				throw new IllegalStateException("This callback is already polling this async refresh");
			existingRecord.callbacks.add(callback);
			return;
		}
		AsyncRefreshPollRecord r=new AsyncRefreshPollRecord();
		r.callbacks.add(callback);
		r.retryInterval=header.retryInterval;
		r.upcomingApiCall=()->doAsyncRefreshPoll(header.id, r);
		asyncRefreshPolls.put(header.id, r);
		thread.postRunnable(r.upcomingApiCall, r.retryInterval*1000);
		if(BuildConfig.DEBUG)
			Log.d(TAG, "Starting async refresh poll for id "+header.id);
	}

	private void doAsyncRefreshPoll(String id, AsyncRefreshPollRecord r){
		r.currentApiCall=new GetAsyncRefresh(id)
				.setCallback(new me.grishka.appkit.api.Callback<>(){
					@Override
					public void onSuccess(GetAsyncRefresh.Response result){
						AsyncRefresh ar=result.asyncRefresh;
						if(ar.status==AsyncRefresh.RefreshStatus.FINISHED){
							for(Consumer<AsyncRefresh> callback:r.callbacks){
								callback.accept(ar);
							}
							asyncRefreshPolls.remove(id);
							r.currentApiCall=null;
						}else if(ar.status==AsyncRefresh.RefreshStatus.RUNNING){
							r.resultCount=ar.resultCount;
							r.currentApiCall=null;
							thread.postRunnable(r.upcomingApiCall, r.retryInterval*1000);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						if(BuildConfig.DEBUG)
							Log.w(TAG, "Async refresh "+id+" polling failed");
						asyncRefreshPolls.remove(id);
						r.currentApiCall=null;
					}
				})
				.exec(session.getID());
	}

	public void cancelPollingAsyncRefresh(String id, Consumer<AsyncRefresh> callback){
		AsyncRefreshPollRecord r=asyncRefreshPolls.get(id);
		if(r==null)
			return;
		r.callbacks.remove(callback);
		if(r.callbacks.isEmpty()){
			if(BuildConfig.DEBUG)
				Log.d(TAG, "Canceling async refresh poll "+id+" because there are no callbacks left");
			asyncRefreshPolls.remove(id);
			thread.handler.removeCallbacks(r.upcomingApiCall);
			if(r.currentApiCall!=null)
				r.currentApiCall.cancel();
		}
	}

	public int getAsyncRefreshResultCount(String id){
		AsyncRefreshPollRecord r=asyncRefreshPolls.get(id);
		return r==null ? 0 : r.resultCount;
	}

	private static class AsyncRefreshPollRecord{
		public ArrayList<Consumer<AsyncRefresh>> callbacks=new ArrayList<>();
		public int retryInterval;
		public Runnable upcomingApiCall;
		public MastodonAPIRequest<?> currentApiCall;
		public int resultCount;
	}
}
