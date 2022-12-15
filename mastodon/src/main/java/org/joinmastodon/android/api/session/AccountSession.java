package org.joinmastodon.android.api.session;

import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.model.Token;

import java.util.ArrayList;
import java.util.List;

public class AccountSession{
	public Token token;
	public Account self;
	public String domain;
	public Application app;
	public long infoLastUpdated;
	public boolean activated=true;
	public String pushPrivateKey;
	public String pushPublicKey;
	public String pushAuthKey;
	public PushSubscription pushSubscription;
	public boolean needUpdatePushSettings;
	public long filtersLastUpdated;
	public List<Filter> wordFilters=new ArrayList<>();
	public String pushAccountID;
	public AccountActivationInfo activationInfo;
	private transient MastodonAPIController apiController;
	private transient StatusInteractionController statusInteractionController;
	private transient CacheController cacheController;
	private transient PushSubscriptionManager pushSubscriptionManager;

	AccountSession(Token token, Account self, Application app, String domain, boolean activated, AccountActivationInfo activationInfo){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.activated=activated;
		this.activationInfo=activationInfo;
		infoLastUpdated=System.currentTimeMillis();
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

	public PushSubscriptionManager getPushSubscriptionManager(){
		if(pushSubscriptionManager==null)
			pushSubscriptionManager=new PushSubscriptionManager(getID());
		return pushSubscriptionManager;
	}
}
