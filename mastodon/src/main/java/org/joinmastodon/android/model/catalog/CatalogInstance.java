package org.joinmastodon.android.model.catalog;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.BaseModel;

import java.net.IDN;
import java.util.List;

@AllFieldsAreRequired
public class CatalogInstance extends BaseModel{
	public String domain;
	public String version;
	public String description;
	public List<String> languages;
	public String region;
	public List<String> categories;
	public String proxiedThumbnail;
	public int totalUsers;
	public int lastWeekUsers;
	public boolean approvalRequired;
	public String language;
	public String category;

	public transient String normalizedDomain;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(domain.startsWith("xn--") || domain.contains(".xn--"))
			normalizedDomain=IDN.toUnicode(domain);
		else
			normalizedDomain=domain;
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
}
