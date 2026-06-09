package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;

public class WrappedCollectionItem extends BaseModel{
	@RequiredField
	public CollectionItem collectionItem;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		collectionItem.postprocess();
	}
}
