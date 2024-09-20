package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;

@Parcel
public class Notification extends BaseModel implements DisplayItemsParent{
	@RequiredField
	public String id;
//	@RequiredField
	public NotificationType type;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Account account;

	public Status status;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		account.postprocess();
		if(status!=null)
			status.postprocess();
	}

	@Override
	public String getID(){
		return id;
	}

	@Override
	public String getAccountID(){
		return status!=null ? account.id : null;
	}
}
