package org.joinmastodon.android.model;

import android.util.Log;

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
	public Account account;
	public Status status;
	public RelationshipSeveranceEvent event;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		account.postprocess();
		if(status!=null)
			status.postprocess();
		if(event!=null){
			try{
				event.postprocess();
			}catch(ObjectValidationException x){
				Log.w("Notification", x);
				event=null;
			}
		}
		if(type!=NotificationType.SEVERED_RELATIONSHIPS && account==null){
			throw new ObjectValidationException("account must be present for type "+type);
		}
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
