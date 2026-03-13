package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.List;

/**
 * Represents display or publishing preferences of user's own account. Returned as an additional entity when verifying and updated credentials, as an attribute of Account.
 */
@Parcel
public class Source extends BaseModel{
	/**
	 * Profile bio.
	 */
	@RequiredField
	public String note;
	/**
	 * Metadata about the account.
	 */
	@RequiredField
	public List<AccountField> fields;
	/**
	 * The default post privacy to be used for new statuses.
	 */
	public StatusPrivacy privacy;
	/**
	 * Whether new statuses should be marked sensitive by default.
	 */
	public boolean sensitive;
	/**
	 * The default posting language for new statuses.
	 */
	public String language;
	/**
	 * The number of pending follow requests.
	 */
	public int followRequestCount;
	public Boolean indexable;
	public boolean hideCollections;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(AccountField f:fields)
			f.postprocess();
	}

	@Override
	public String toString(){
		return "Source{"+
				"note='"+note+'\''+
				", fields="+fields+
				", privacy="+privacy+
				", sensitive="+sensitive+
				", language='"+language+'\''+
				", followRequestCount="+followRequestCount+
				", indexable="+indexable+
				", hideCollections="+hideCollections+
				'}';
	}
}
