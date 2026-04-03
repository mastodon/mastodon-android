package org.joinmastodon.android.api.requests;

import org.joinmastodon.android.api.MastodonAPIRequest;

import java.util.HashMap;
import java.util.Map;

import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

/**
 * A wrapper to execute multiple API requests concurrently and receive their results in a single callback.
 * If one of the requests fails, the whole thing fails.
 */
public class BatchRequest extends APIRequest<Map<String, Object>>{
	private final Map<String, MastodonAPIRequest<?>> requests;
	private final HashMap<String, MastodonAPIRequest<?>> runningRequests=new HashMap<>();
	private final HashMap<String, Object> results=new HashMap<>();

	public BatchRequest(Map<String, MastodonAPIRequest<?>> requests){
		this.requests=requests;
	}

	@Override
	public void cancel(){
		for(MastodonAPIRequest<?> req:runningRequests.values()){
			req.cancel();
		}
	}

	@Override
	public APIRequest<Map<String, Object>> exec(){
		throw new UnsupportedOperationException("Use exec(accountID) instead");
	}

	@Override
	public BatchRequest setCallback(Callback<Map<String, Object>> callback){
		super.setCallback(callback);
		return this;
	}

	public BatchRequest exec(String accountID){
		if(requests.isEmpty())
			throw new IllegalStateException("No requests to execute");
		runningRequests.putAll(requests);
		requests.forEach((key, req)->((MastodonAPIRequest<Object>)req).setCallback(new RequestCallback(key)));
		for(MastodonAPIRequest<?> req:requests.values()){
			req.exec(accountID);
		}
		return this;
	}

	private class RequestCallback implements Callback<Object>{
		private final String key;

		private RequestCallback(String key){
			this.key=key;
		}

		@Override
		public void onSuccess(Object result){
			runningRequests.remove(key);
			results.put(key, result);
			if(runningRequests.isEmpty())
				invokeSuccessCallback(results);
		}

		@Override
		public void onError(ErrorResponse error){
			for(MastodonAPIRequest<?> req:runningRequests.values()){
				req.cancel();
			}
			invokeErrorCallback(error);
		}
	}
}
