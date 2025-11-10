package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.AsyncRefreshHeader;
import org.joinmastodon.android.api.ObjectValidationException;

import java.util.List;

@AllFieldsAreRequired
public class StatusContext extends BaseModel{
	public List<Status> ancestors;
	public List<Status> descendants;
	public transient AsyncRefreshHeader asyncRefresh;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(Status s:ancestors)
			s.postprocess();
		for(Status s:descendants)
			s.postprocess();
	}
}
