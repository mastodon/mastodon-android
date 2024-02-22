package com.google.android.gms.common.moduleinstall;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ModuleInstallResponse extends AutoSafeParcelable{
	@SafeParceled(1)
	public int sessionID;
	@SafeParceled(2)
	public boolean shouldUnregisterListener;

	public static final Creator<ModuleInstallResponse> CREATOR=new AutoCreator<>(ModuleInstallResponse.class);

	@Override
	public String toString(){
		return "ModuleInstallResponse{"+
				"sessionID="+sessionID+
				", shouldUnregisterListener="+shouldUnregisterListener+
				'}';
	}
}
