package com.google.android.gms.common.moduleinstall;

import android.app.PendingIntent;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ModuleInstallIntentResponse extends AutoSafeParcelable{
	@SafeParceled(1)
	public PendingIntent pendingIntent;

	public static final Creator<ModuleInstallIntentResponse> CREATOR=new AutoCreator<>(ModuleInstallIntentResponse.class);
}
