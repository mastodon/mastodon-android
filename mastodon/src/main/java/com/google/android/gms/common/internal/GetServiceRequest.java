package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.accounts.Account;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.Scope;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class GetServiceRequest extends AutoSafeParcelable{
	@SafeParceled(1)
	int versionCode=6;
	@SafeParceled(2)
	public int serviceId;
	@SafeParceled(3)
	public int gmsVersion;
	@SafeParceled(4)
	public String packageName;
	@SafeParceled(5)
	public IBinder accountAccessor;
	@SafeParceled(6)
	public Scope[] scopes;
	@SafeParceled(7)
	public Bundle extras;
	@SafeParceled(8)
	public Account account;
	@SafeParceled(9)
	@Deprecated
	long field9;
	@SafeParceled(10)
	public Feature[] defaultFeatures;
	@SafeParceled(11)
	public Feature[] apiFeatures;
	@SafeParceled(12)
	boolean supportsConnectionInfo;
	@SafeParceled(13)
	int field13;
	@SafeParceled(14)
	boolean field14;
	@SafeParceled(15)
	String attributionTag;

	public static final Creator<GetServiceRequest> CREATOR=new AutoCreator<>(GetServiceRequest.class);
}
