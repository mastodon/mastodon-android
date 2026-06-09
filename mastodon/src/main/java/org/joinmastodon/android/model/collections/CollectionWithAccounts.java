package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.BaseModel;

import java.util.List;

@AllFieldsAreRequired
public class CollectionWithAccounts extends BaseModel{
	public List<Account> accounts;
	public AccountCollection collection;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(Account account:accounts){
			account.postprocess();
		}
		collection.postprocess();
	}
}
