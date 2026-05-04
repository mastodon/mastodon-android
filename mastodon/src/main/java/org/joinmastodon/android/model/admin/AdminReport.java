package org.joinmastodon.android.model.admin;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.BaseModel;

public class AdminReport extends BaseModel{
	// Note: this only contains the fields that are relevant for the app, so it could display "admin.report" notifications
	@RequiredField
	public String id;
	public String comment;
	@RequiredField
	public Account targetAccount;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		targetAccount.postprocess();
	}
}
