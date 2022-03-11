package org.joinmastodon.android.api.session;

import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Token;

public class AccountSession{
	public Token token;
	public Account self;
	public String domain;
	public int tootCharLimit;
	public Application app;
	public long infoLastUpdated;
	public long instanceLastUpdated;
	public Instance instance;
	public boolean activated=true;
	private transient MastodonAPIController apiController;
	private transient StatusInteractionController statusInteractionController;
	private transient CacheController cacheController;

	AccountSession(Token token, Account self, Application app, String domain, int tootCharLimit, Instance instance, boolean activated){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.tootCharLimit=tootCharLimit;
		this.instance=instance;
		this.activated=activated;
		instanceLastUpdated=infoLastUpdated=System.currentTimeMillis();
	}

	AccountSession(){}

	public String getID(){
		return domain+"_"+self.id;
	}

	public MastodonAPIController getApiController(){
		if(apiController==null)
			apiController=new MastodonAPIController(this);
		return apiController;
	}

	public StatusInteractionController getStatusInteractionController(){
		if(statusInteractionController==null)
			statusInteractionController=new StatusInteractionController(getID());
		return statusInteractionController;
	}

	public CacheController getCacheController(){
		if(cacheController==null)
			cacheController=new CacheController(getID());
		return cacheController;
	}
}
