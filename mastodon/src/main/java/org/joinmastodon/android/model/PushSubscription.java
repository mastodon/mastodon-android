package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;

@AllFieldsAreRequired
public class PushSubscription extends BaseModel{
	public int id;
	public String endpoint;
	public Alerts alerts;
	public String serverKey;

	@Override
	public String toString(){
		return "PushSubscription{"+
				"id="+id+
				", endpoint='"+endpoint+'\''+
				", alerts="+alerts+
				", serverKey='"+serverKey+'\''+
				'}';
	}

	public static class Alerts{
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
	}
}
