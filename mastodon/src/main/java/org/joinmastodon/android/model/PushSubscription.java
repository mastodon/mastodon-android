package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.AllFieldsAreRequired;

import androidx.annotation.NonNull;

@AllFieldsAreRequired
public class PushSubscription extends BaseModel implements Cloneable{
	public int id;
	public String endpoint;
	public Alerts alerts;
	public String serverKey;
	public Policy policy=Policy.ALL;

	public PushSubscription(){}

	@Override
	public String toString(){
		return "PushSubscription{"+
				"id="+id+
				", endpoint='"+endpoint+'\''+
				", alerts="+alerts+
				", serverKey='"+serverKey+'\''+
				", policy="+policy+
				'}';
	}

	@NonNull
	@Override
	public PushSubscription clone(){
		PushSubscription copy=(PushSubscription) super.clone();
		copy.alerts=alerts.clone();
		return copy;
	}

	public static class Alerts implements Cloneable{
		public boolean follow;
		public boolean favourite;
		public boolean reblog;
		public boolean mention;
		public boolean poll;

		public static Alerts ofAll(){
			Alerts alerts=new Alerts();
			alerts.follow=alerts.favourite=alerts.reblog=alerts.mention=alerts.poll=true;
			return alerts;
		}

		@Override
		public String toString(){
			return "Alerts{"+
					"follow="+follow+
					", favourite="+favourite+
					", reblog="+reblog+
					", mention="+mention+
					", poll="+poll+
					'}';
		}

		@NonNull
		@Override
		public Alerts clone(){
			try{
				return (Alerts) super.clone();
			}catch(CloneNotSupportedException e){
				return null;
			}
		}
	}

	public enum Policy{
		@SerializedName("all")
		ALL,
		@SerializedName("followed")
		FOLLOWED,
		@SerializedName("follower")
		FOLLOWER,
		@SerializedName("none")
		NONE
	}
}
