package org.joinmastodon.android.model.catalog;

import android.graphics.Region;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.BaseModel;

import java.net.IDN;
import java.util.List;

import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

@AllFieldsAreRequired
public class CatalogInstance extends BaseModel{
	public String domain;
	public String version;
	public String description;
	public List<String> languages;
	@SerializedName("region")
	private String _region;
	public List<String> categories;
	public String proxiedThumbnail;
	public int totalUsers;
	public int lastWeekUsers;
	public boolean approvalRequired;
	public String language;
	public String category;

	public transient Region region;
	public transient String normalizedDomain;
	public transient UrlImageLoaderRequest thumbnailRequest;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(domain.startsWith("xn--") || domain.contains(".xn--"))
			normalizedDomain=IDN.toUnicode(domain);
		else
			normalizedDomain=domain;
		if(!TextUtils.isEmpty(_region)){
			try{
				region=Region.valueOf(_region.toUpperCase());
			}catch(IllegalArgumentException ignore){}
		}
		if(!TextUtils.isEmpty(proxiedThumbnail)){
			thumbnailRequest=new UrlImageLoaderRequest(proxiedThumbnail, 0, V.dp(56));
		}
	}

	@Override
	public String toString(){
		return "CatalogInstance{"+
				"domain='"+domain+'\''+
				", version='"+version+'\''+
				", description='"+description+'\''+
				", languages="+languages+
				", region='"+region+'\''+
				", categories="+categories+
				", proxiedThumbnail='"+proxiedThumbnail+'\''+
				", totalUsers="+totalUsers+
				", lastWeekUsers="+lastWeekUsers+
				", approvalRequired="+approvalRequired+
				", language='"+language+'\''+
				", category='"+category+'\''+
				'}';
	}

	public enum Region{
		EUROPE,
		NORTH_AMERICA,
		SOUTH_AMERICA,
		AFRICA,
		ASIA,
		OCEANIA
	}
}
