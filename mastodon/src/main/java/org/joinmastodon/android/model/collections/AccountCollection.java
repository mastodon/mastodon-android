package org.joinmastodon.android.model.collections;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.Hashtag;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;

// Called such to avoid conflict with java.util.Collection
@Parcel
public class AccountCollection extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public String accountId;
	@RequiredField
	public String name;
	public String description;
	public String language;
	public boolean local;
	@RequiredField
	public String url;
	public boolean sensitive;
	public boolean discoverable;
	public Hashtag tag;
	public int itemCount;
	@RequiredField
	public List<CollectionItem> items;
	@RequiredField
	public Instant createdAt;
	public Instant updatedAt;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(tag!=null)
			tag.postprocess();
		for(CollectionItem item:items){
			item.postprocess();
		}
	}
}
