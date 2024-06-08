package org.joinmastodon.android.api.requests.catalog;

import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.catalog.CatalogInstance;

import java.util.List;

public class GetCatalogInstances extends MastodonAPIRequest<List<CatalogInstance>>{

	private String lang, category;
	private boolean includeClosedSignups;

	public GetCatalogInstances(String lang, String category, boolean includeClosedSignups){
		super(HttpMethod.GET, null, new TypeToken<>(){});
		this.lang=lang;
		this.category=category;
		this.includeClosedSignups=includeClosedSignups;
	}

	@Override
	public Uri getURL(){
		Uri.Builder builder=new Uri.Builder()
				.scheme("https")
				.authority("api.joinmastodon.org")
				.path("/servers");
		if(!TextUtils.isEmpty(lang))
			builder.appendQueryParameter("language", lang);
		if(!TextUtils.isEmpty(category))
			builder.appendQueryParameter("category", category);
		if(includeClosedSignups)
			builder.appendQueryParameter("registrations", "all");
		return builder.build();
	}
}
