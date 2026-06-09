package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.PartialAccount;

import java.util.List;

public class AccountCollections extends BaseModel{
	public List<AccountCollection> collections;
	public List<PartialAccount> partialAccounts;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(AccountCollection collection:collections){
			collection.postprocess();
		}
		for(PartialAccount acc:partialAccounts){
			acc.postprocess();
		}
	}
}
