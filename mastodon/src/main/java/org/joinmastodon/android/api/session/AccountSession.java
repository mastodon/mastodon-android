package org.joinmastodon.android.api.session;

import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Token;

public class AccountSession{
	public Token token;
	public Account self;
	public String domain;
	public int tootCharLimit;
	public Application app;
	public long infoLastUpdated;
	private transient MastodonAPIController apiController;
	private transient StatusInteractionController statusInteractionController;

	AccountSession(Token token, Account self, Application app, String domain, int tootCharLimit){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.tootCharLimit=tootCharLimit;
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
}
