package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;

import java.util.List;

public class Hashtag extends BaseModel{
	@RequiredField
	public String name;
	@RequiredField
	public String url;
	public List<History> history;

	@Override
	public String toString(){
		return "Hashtag{"+
				"name='"+name+'\''+
				", url='"+url+'\''+
				", history="+history+
				'}';
	}
}
