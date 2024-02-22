package com.google.android.gms.common.moduleinstall.internal;

import com.google.android.gms.common.Feature;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class ApiFeatureRequest extends AutoSafeParcelable{
	@SafeParceled(value=1, subClass=Feature.class)
	public List<Feature> features;
	@SafeParceled(2)
	public boolean urgent;
	@SafeParceled(3)
	public String sessionId;
	@SafeParceled(4)
	public String callingPackage;

	public static final Creator<ApiFeatureRequest> CREATOR=new AutoCreator<>(ApiFeatureRequest.class);
}
