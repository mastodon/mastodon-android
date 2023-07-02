package org.joinmastodon.android.model.catalog;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.model.BaseModel;

@AllFieldsAreRequired
public class CatalogDefaultInstance extends BaseModel{
	public String domain;
	public float weight;
}
