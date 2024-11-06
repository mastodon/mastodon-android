package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;

import java.util.List;

@AllFieldsAreRequired
public class FamiliarFollowers extends BaseModel{
	public String id;
	public List<Account> accounts;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(Account acc:accounts){
			acc.postprocess();
		}
	}
}
