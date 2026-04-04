package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;

import java.util.List;

/**
 * Used for profile editing (returned by GET /api/v1/profile and PUT /api/v1/profile)
 */
public class Profile extends BaseModel{
	@RequiredField
	public String id;
	public String displayName;
	public String note;
	@RequiredField
	public List<AccountField> fields;
	public String avatar;
	public String avatarStatic;
	public String avatarDescription;
	public String header;
	public String headerStatic;
	public String headerDescription;
	public boolean locked;
	public boolean bot;
	public boolean hideCollections;
	public boolean discoverable;
	public boolean indexable;
	public boolean showMedia;
	public boolean showMediaReplies;
	public boolean showFeatured;
	// attribution_domains
	@RequiredField
	public List<Hashtag> featuredTags;

	public Profile(){}

	public Profile(Account acc){
		id=acc.id;
		displayName=acc.displayName;
		note=acc.source.note;
		fields=acc.source.fields;
		avatar=acc.avatar;
		avatarStatic=acc.avatarStatic;
		header=acc.header;
		headerStatic=acc.headerStatic;
		locked=acc.locked;
		bot=acc.bot;
		hideCollections=acc.source.hideCollections;
		discoverable=acc.discoverable;
		indexable=acc.source.indexable;
		showMedia=acc.showMedia;
		showMediaReplies=acc.showMediaReplies;
		showFeatured=acc.showFeatured;
		featuredTags=List.of();
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(AccountField f:fields)
			f.postprocess();
		for(Hashtag t:featuredTags)
			t.postprocess();
	}
}
