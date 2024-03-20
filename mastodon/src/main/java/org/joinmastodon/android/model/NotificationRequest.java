package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;

import java.time.Instant;

public class NotificationRequest extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Instant updatedAt;
	public int notificationsCount;
	@RequiredField
	public Account account;
	public Status lastStatus;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		account.postprocess();
		if(lastStatus!=null)
			lastStatus.postprocess();
	}
}
