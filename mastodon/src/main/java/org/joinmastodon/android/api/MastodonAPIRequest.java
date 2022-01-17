package org.joinmastodon.android.api;

import android.net.Uri;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.Token;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.CallSuper;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import okhttp3.Call;
import okhttp3.RequestBody;

public abstract class MastodonAPIRequest<T> extends APIRequest<T>{

	private String domain;
	private AccountSession account;
	private String path;
	private String method;
	private Object requestBody;
	private Map<String, String> queryParams;
	Class<T> respClass;
	TypeToken<T> respTypeToken;
	Call okhttpCall;
	Token token;
	boolean canceled;

	public MastodonAPIRequest(HttpMethod method, String path, Class<T> respClass){
		this.path=path;
		this.method=method.toString();
		this.respClass=respClass;
	}

	public MastodonAPIRequest(HttpMethod method, String path, TypeToken<T> respTypeToken){
		this.path=path;
		this.method=method.toString();
		this.respTypeToken=respTypeToken;
	}

	@Override
	public synchronized void cancel(){
		canceled=true;
		if(okhttpCall!=null){
			okhttpCall.cancel();
		}
	}

	@Override
	public APIRequest<T> exec(){
		throw new UnsupportedOperationException("Use exec(accountID) or execNoAuth(domain)");
	}

	public MastodonAPIRequest<T> exec(String accountID){
		account=AccountSessionManager.getInstance().getAccount(accountID);
		domain=account.domain;
		account.getApiController().submitRequest(this);
		return this;
	}

	public MastodonAPIRequest<T> execNoAuth(String domain){
		this.domain=domain;
		AccountSessionManager.getInstance().getUnauthenticatedApiController().submitRequest(this);
		return this;
	}

	public MastodonAPIRequest<T> exec(String domain, Token token){
		this.domain=domain;
		this.token=token;
		AccountSessionManager.getInstance().getUnauthenticatedApiController().submitRequest(this);
		return this;
	}

	protected void setRequestBody(Object body){
		requestBody=body;
	}

	protected void addQueryParameter(String key, String value){
		if(queryParams==null)
			queryParams=new HashMap<>();
		queryParams.put(key, value);
	}

	protected String getPathPrefix(){
		return "/api/v1";
	}

	public Uri getURL(){
		Uri.Builder builder=new Uri.Builder()
				.scheme("https")
				.authority(domain)
				.path(getPathPrefix()+path);
		if(queryParams!=null){
			for(Map.Entry<String, String> param:queryParams.entrySet()){
				builder.appendQueryParameter(param.getKey(), param.getValue());
			}
		}
		return builder.build();
	}

	public String getMethod(){
		return method;
	}

	public RequestBody getRequestBody(){
		return requestBody==null ? null : new JsonObjectRequestBody(requestBody);
	}

	@Override
	public MastodonAPIRequest<T> setCallback(Callback<T> callback){
		super.setCallback(callback);
		return this;
	}

	@CallSuper
	public void validateAndPostprocessResponse(T respObj) throws IOException{
		if(respObj instanceof BaseModel){
			((BaseModel) respObj).postprocess();
		}else if(respObj instanceof List){
			for(Object item : ((List) respObj)){
				if(item instanceof BaseModel)
					((BaseModel) item).postprocess();
			}
		}
	}

	void onError(String msg){
		invokeErrorCallback(new MastodonErrorResponse(msg));
	}

	void onSuccess(T resp){
		invokeSuccessCallback(resp);
	}

	public enum HttpMethod{
		GET,
		POST,
		PUT,
		DELETE
	}
}
