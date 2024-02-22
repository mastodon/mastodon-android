package com.google.android.gms.common.moduleinstall;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ModuleAvailabilityResponse extends AutoSafeParcelable{
	@SafeParceled(1)
	public boolean modulesAvailable;
	@SafeParceled(2)
	public int availabilityStatus;

	public static final Creator<ModuleAvailabilityResponse> CREATOR=new AutoCreator<>(ModuleAvailabilityResponse.class);
}
