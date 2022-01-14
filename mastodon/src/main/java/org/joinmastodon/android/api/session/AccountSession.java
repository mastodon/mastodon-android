package org.joinmastodon.android.api.session;

import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Token;

public class AccountSession{
	public Token token;
	public Account self;
	public String domain;
	public int tootCharLimit;
	public Application app;
	private transient MastodonAPIController apiController;

	AccountSession(Token token, Account self, Application app, String domain, int tootCharLimit){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.tootCharLimit=tootCharLimit;
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
}
