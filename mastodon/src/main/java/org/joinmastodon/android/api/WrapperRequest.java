package org.joinmastodon.android.api;

import me.grishka.appkit.api.APIRequest;

/**
 * Wraps a different API request to allow a chain of requests to be canceled
 */
public class WrapperRequest<T> extends APIRequest<T>{
	public APIRequest<?> wrappedRequest;

	@Override
	public void cancel(){
		if(wrappedRequest!=null)
			wrappedRequest.cancel();
	}

	@Override
	public APIRequest<T> exec(){
		throw new UnsupportedOperationException();
	}
}
