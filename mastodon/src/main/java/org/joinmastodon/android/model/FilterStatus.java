package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.parceler.Parcel;

@AllFieldsAreRequired
@Parcel
public class FilterStatus extends BaseModel{
	public String id;
	public String statusId;
}
