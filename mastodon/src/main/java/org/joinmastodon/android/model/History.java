package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;

@AllFieldsAreRequired
public class History extends BaseModel{
	public long day; // unixtime
	public int uses;
	public int accounts;

	@Override
	public String toString(){
		return "History{"+
				"day="+day+
				", uses="+uses+
				", accounts="+accounts+
				'}';
	}
}
