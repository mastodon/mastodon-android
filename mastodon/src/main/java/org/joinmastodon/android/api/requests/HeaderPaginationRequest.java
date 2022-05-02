package org.joinmastodon.android.api.requests;

import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.HeaderPaginationList;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;

public abstract class HeaderPaginationRequest<I> extends MastodonAPIRequest<HeaderPaginationList<I>>{
	private static final Pattern LINK_HEADER_PATTERN=Pattern.compile("(?:(?:,\\s*)?<([^>]+)>|;\\s*(\\w+)=['\"](\\w+)['\"])");

	public HeaderPaginationRequest(HttpMethod method, String path, Class<HeaderPaginationList<I>> respClass){
		super(method, path, respClass);
	}

	public HeaderPaginationRequest(HttpMethod method, String path, TypeToken<HeaderPaginationList<I>> respTypeToken){
		super(method, path, respTypeToken);
	}

	@Override
	public void validateAndPostprocessResponse(HeaderPaginationList<I> respObj, Response httpResponse) throws IOException{
		super.validateAndPostprocessResponse(respObj, httpResponse);
		String link=httpResponse.header("Link");
		if(!TextUtils.isEmpty(link)){
			Matcher matcher=LINK_HEADER_PATTERN.matcher(link);
			String url=null;
			while(matcher.find()){
				if(url==null){
					String _url=matcher.group(1);
					if(_url==null)
						continue;
					url=_url;
				}else{
					String paramName=matcher.group(2);
					String paramValue=matcher.group(3);
					if(paramName==null || paramValue==null)
						return;
					if("rel".equals(paramName)){
						switch(paramValue){
							case "next" -> respObj.nextPageUri=Uri.parse(url);
							case "prev" -> respObj.prevPageUri=Uri.parse(url);
						}
						url=null;
					}
				}
			}
		}
	}
}
