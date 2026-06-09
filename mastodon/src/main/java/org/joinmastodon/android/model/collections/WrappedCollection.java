package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;

public class WrappedCollection extends BaseModel{
	@RequiredField
	public AccountCollection collection;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		collection.postprocess();
	}
}
