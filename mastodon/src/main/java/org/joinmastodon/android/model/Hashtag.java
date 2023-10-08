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

	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(o==null || getClass()!=o.getClass()) return false;

		Hashtag hashtag=(Hashtag) o;

		return name.equals(hashtag.name);
	}

	@Override
	public int hashCode(){
		return name.hashCode();
	}

	public int getWeekPosts(){
		return history.stream().mapToInt(h->h.uses).sum();
	}
}
