package com.google.android.gms.common.api;

import android.app.PendingIntent;

import org.joinmastodon.android.googleservices.ConnectionResult;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class Status extends AutoSafeParcelable{
	@SafeParceled(1000)
	public int versionCode;
	@SafeParceled(1)
	public int statusCode;
	@SafeParceled(2)
	public String statusMessage;
	@SafeParceled(3)
	public PendingIntent pendingIntent;
	@SafeParceled(4)
	public ConnectionResult connectionResult;

	public static final Creator<Status> CREATOR=new AutoCreator<>(Status.class);

	@Override
	public String toString(){
		return "Status{"+
				"versionCode="+versionCode+
				", statusCode="+statusCode+
				", statusMessage='"+statusMessage+'\''+
				", pendingIntent="+pendingIntent+
				", connectionResult="+connectionResult+
				'}';
	}
}
