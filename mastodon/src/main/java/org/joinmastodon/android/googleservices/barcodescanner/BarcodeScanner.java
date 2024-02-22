package org.joinmastodon.android.googleservices.barcodescanner;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;

import org.joinmastodon.android.MastodonApp;

public class BarcodeScanner{
	public static Intent createIntent(int formats, boolean allowManualInout, boolean enableAutoZoom){
		Intent intent=new Intent().setPackage("com.google.android.gms").setAction("com.google.android.gms.mlkit.ACTION_SCAN_BARCODE");
		String appName;
		ApplicationInfo appInfo=MastodonApp.context.getApplicationInfo();
		if(appInfo.labelRes!=0)
			appName=MastodonApp.context.getString(appInfo.labelRes);
		else
			appName=MastodonApp.context.getPackageManager().getApplicationLabel(appInfo).toString();
		intent.putExtra("extra_calling_app_name", appName);
		intent.putExtra("extra_supported_formats", formats);
		intent.putExtra("extra_allow_manual_input", allowManualInout);
		intent.putExtra("extra_enable_auto_zoom", enableAutoZoom);
		return intent;
	}

	public static boolean isValidResult(Intent intent){
		return intent!=null && intent.hasExtra("extra_barcode_result");
	}

	public static Barcode getResult(Intent intent){
		byte[] serialized=intent.getByteArrayExtra("extra_barcode_result");
		Parcel parcel=Parcel.obtain();
		parcel.unmarshall(serialized, 0, serialized.length);
		parcel.setDataPosition(0);
		Barcode barcode=Barcode.CREATOR.createFromParcel(parcel);
		parcel.recycle();
		return barcode;
	}
}
