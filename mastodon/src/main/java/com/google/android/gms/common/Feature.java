package com.google.android.gms.common;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class Feature extends AutoSafeParcelable{
	@SafeParceled(1)
	public String name;
	@SafeParceled(2)
	public int oldVersion;
	@SafeParceled(3)
	public long version=-1;

	public static final Creator<Feature> CREATOR=new AutoCreator<>(Feature.class);
}
