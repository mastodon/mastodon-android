package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.model.BaseModel;
import org.parceler.Parcel;

import java.time.Instant;

@AllFieldsAreRequired
@Parcel
public class CollectionItem extends BaseModel{
	public String id;
	public String accountId;
	public CollectionInclusionState state=CollectionInclusionState.PENDING;
	public Instant createdAt;
}
