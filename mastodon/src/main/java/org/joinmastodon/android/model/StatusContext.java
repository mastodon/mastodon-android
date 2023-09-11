package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;

import java.util.List;

@AllFieldsAreRequired
public class StatusContext extends BaseModel{
	public List<Status> ancestors;
	public List<Status> descendants;
}
