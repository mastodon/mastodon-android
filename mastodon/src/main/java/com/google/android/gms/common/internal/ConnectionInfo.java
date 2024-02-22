package com.google.android.gms.common.internal;

import android.os.Bundle;

import com.google.android.gms.common.Feature;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConnectionInfo extends AutoSafeParcelable{
	@SafeParceled(1)
	public Bundle params;
	@SafeParceled(2)
	public Feature[] features;
	@SafeParceled(3)
	public int unknown3;

	public static final Creator<ConnectionInfo> CREATOR=new AutoCreator<>(ConnectionInfo.class);
}
