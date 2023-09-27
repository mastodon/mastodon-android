package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.parceler.Parcel;

@AllFieldsAreRequired
@Parcel
public class Mention extends BaseModel{
	public String id;
	public String username;
	public String acct;
	public String url;

	@Override
	public String toString(){
		return "Mention{"+
				"id='"+id+'\''+
				", username='"+username+'\''+
				", acct='"+acct+'\''+
				", url='"+url+'\''+
				'}';
	}

	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(o==null || getClass()!=o.getClass()) return false;

		Mention mention=(Mention) o;

		if(!id.equals(mention.id)) return false;
		return url.equals(mention.url);
	}

	@Override
	public int hashCode(){
		int result=id.hashCode();
		result=31*result+url.hashCode();
		return result;
	}
}
