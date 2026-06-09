package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.collections.AccountCollection;

import java.util.List;

public class SearchResults extends BaseModel{
	public List<Account> accounts;
	public List<Status> statuses;
	public List<Hashtag> hashtags;
	public List<AccountCollection> collections;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(accounts!=null){
			for(Account acc:accounts)
				acc.postprocess();
		}
		if(statuses!=null){
			for(Status s:statuses)
				s.postprocess();
		}
		if(hashtags!=null){
			for(Hashtag t:hashtags)
				t.postprocess();
		}
		if(collections!=null){
			for(AccountCollection c:collections)
				c.postprocess();
		}
	}
}
