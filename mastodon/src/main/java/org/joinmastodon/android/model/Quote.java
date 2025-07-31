package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class Quote extends BaseModel{
	@RequiredField
	public State state;
	public Status quotedStatus;
	public String quotedStatusId;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(quotedStatus!=null)
			quotedStatus.postprocess();
	}

	public enum State{
		@SerializedName("pending")
		PENDING,
		@SerializedName("accepted")
		ACCEPTED,
		@SerializedName("rejected")
		REJECTED,
		@SerializedName("revoked")
		REVOKED,
		@SerializedName("deleted")
		DELETED,
		@SerializedName("unauthorized")
		UNAUTHORIZED
	}
}
