package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;

public class FollowSuggestion extends BaseModel {
    @RequiredField
    public Account account;
//	public String source;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        account.postprocess();
    }
}
