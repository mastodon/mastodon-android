package org.joinmastodon.android.googleservices;

import android.app.PendingIntent;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConnectionResult extends AutoSafeParcelable{
	public static final int UNKNOWN = -1;
	public static final int SUCCESS = 0;
	public static final int SERVICE_MISSING = 1;
	public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
	public static final int SERVICE_DISABLED = 3;
	public static final int SIGN_IN_REQUIRED = 4;
	public static final int INVALID_ACCOUNT = 5;
	public static final int RESOLUTION_REQUIRED = 6;
	public static final int NETWORK_ERROR = 7;
	public static final int INTERNAL_ERROR = 8;
	public static final int SERVICE_INVALID = 9;
	public static final int DEVELOPER_ERROR = 10;
	public static final int LICENSE_CHECK_FAILED = 11;
	public static final int CANCELED = 13;
	public static final int TIMEOUT = 14;
	public static final int INTERRUPTED = 15;
	public static final int API_UNAVAILABLE = 16;
	public static final int SIGN_IN_FAILED = 17;
	public static final int SERVICE_UPDATING = 18;
	public static final int SERVICE_MISSING_PERMISSION = 19;
	public static final int RESTRICTED_PROFILE = 20;
	public static final int RESOLUTION_ACTIVITY_NOT_FOUND = 22;
	public static final int API_DISABLED = 23;
	public static final int API_DISABLED_FOR_CONNECTION = 24;
	@Deprecated
	public static final int DRIVE_EXTERNAL_STORAGE_REQUIRED = 1500;


	@SafeParceled(1)
	public int versionCode;
	@SafeParceled(2)
	public int errorCode;
	@SafeParceled(3)
	public PendingIntent resolution;
	@SafeParceled(4)
	public String errorMessage;

	public static final Creator<ConnectionResult> CREATOR=new AutoCreator<>(ConnectionResult.class);
}
