package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.PartialAccount;

import java.util.List;

public class AccountCollections extends BaseModel{
	@RequiredField
	public List<AccountCollection> collections;
	public List<PartialAccount> partialAccounts=List.of();

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
