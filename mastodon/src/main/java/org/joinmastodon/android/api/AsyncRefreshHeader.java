package org.joinmastodon.android.api;

import android.text.TextUtils;
import android.util.Log;

import org.joinmastodon.android.BuildConfig;

import java.util.Map;

import okhttp3.Response;

public class AsyncRefreshHeader{
	private static final String TAG="AsyncRefreshHeader";

	public String id;
	public int retryInterval;
	public int resultCount;

	public static AsyncRefreshHeader fromHttpResponse(Response resp){
		String asyncRefresh=resp.header("mastodon-async-refresh");
		if(!TextUtils.isEmpty(asyncRefresh)){
			try{
				Map<String, StructuredHttpHeaders.ItemOrInnerList> refreshParams=StructuredHttpHeaders.parseDictionary(asyncRefresh);
				if(refreshParams.get("id") instanceof StructuredHttpHeaders.Item idItem && idItem.item() instanceof StructuredHttpHeaders.BareItem.StringItem idStrItem){
					AsyncRefreshHeader header=new AsyncRefreshHeader();
					header.id=idStrItem.value();
					if(refreshParams.get("retry") instanceof StructuredHttpHeaders.Item retryItem && retryItem.item() instanceof StructuredHttpHeaders.BareItem.IntegerItem retryIntItem)
						header.retryInterval=(int)retryIntItem.value();
					if(refreshParams.get("result_count") instanceof StructuredHttpHeaders.Item resultCountItem && resultCountItem.item() instanceof StructuredHttpHeaders.BareItem.IntegerItem resultCountIntItem)
						header.resultCount=(int)resultCountIntItem.value();
					return header;
				}
			}catch(IllegalArgumentException x){
				if(BuildConfig.DEBUG)
					Log.w(TAG, "Failed to parse the Mastodon-Async-Refresh header", x);
			}
		}
		return null;
	}
}
