package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.AllFieldsAreRequired;

@AllFieldsAreRequired
public class AsyncRefresh extends BaseModel{
	public String id;
	public RefreshStatus status;
	public int resultCount;

	public enum RefreshStatus{
		@SerializedName("running")
		RUNNING,
		@SerializedName("finished")
		FINISHED,
	}
}
