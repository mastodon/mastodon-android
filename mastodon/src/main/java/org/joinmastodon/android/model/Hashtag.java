package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.List;

@Parcel
public class Hashtag extends BaseModel implements DisplayItemsParent{
	@RequiredField
	public String name;
	@RequiredField
	public String url;
	public List<History> history;
	public int statusesCount;
	public boolean following;

	@Override
	public String toString(){
		return "Hashtag{"+
				"name='"+name+'\''+
				", url='"+url+'\''+
				", history="+history+
				", statusesCount="+statusesCount+
				", following="+following+
				'}';
	}

	@Override
	public String getID(){
		return name;
	}
}
