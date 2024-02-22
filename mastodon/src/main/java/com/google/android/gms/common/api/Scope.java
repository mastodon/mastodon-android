package com.google.android.gms.common.api;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class Scope extends AutoSafeParcelable{
	@SafeParceled(1)
	public int versionCode=1;
	@SafeParceled(2)
	public String scopeUri;

	public static final Creator<Scope> CREATOR=new AutoCreator<>(Scope.class);
}
