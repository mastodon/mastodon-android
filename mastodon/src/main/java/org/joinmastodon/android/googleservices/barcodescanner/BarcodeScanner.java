package org.joinmastodon.android.googleservices.barcodescanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.googleservices.GmsClient;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

public class BarcodeScanner{
	private static final String TAG="BarcodeScanner";

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
	
	public static void installScannerModule(Context context, Runnable onSuccess){
		ProgressDialog progress=new ProgressDialog(context);
		progress.setMessage(context.getString(R.string.loading));
		progress.setCancelable(false);
		progress.show();
		GmsClient.getModuleInstallerService(context, new GmsClient.ServiceConnectionCallback<>(){
			@Override
			public void onSuccess(IModuleInstallService service, int connectionID){
				ApiFeatureRequest req=new ApiFeatureRequest();
				req.callingPackage=context.getPackageName();
				Feature feature=new Feature();
				feature.name="mlkit.barcode.ui";
				feature.version=1;
				feature.oldVersion=-1;
				req.features=List.of(feature);
				req.urgent=true;
				try{
					service.installModules(new IModuleInstallCallbacks.Stub(){
						@Override
						public void onModuleAvailabilityResponse(Status status, ModuleAvailabilityResponse response) throws RemoteException{}

						@Override
						public void onModuleInstallResponse(Status status, ModuleInstallResponse response) throws RemoteException{}

						@Override
						public void onModuleInstallIntentResponse(Status status, ModuleInstallIntentResponse response) throws RemoteException{}

						@Override
						public void onStatus(Status status) throws RemoteException{}
					}, req, new IModuleInstallStatusListener.Stub(){
						@Override
						public void onModuleInstallStatusUpdate(ModuleInstallStatusUpdate statusUpdate) throws RemoteException{
							if(statusUpdate.installState==ModuleInstallStatusUpdate.STATE_COMPLETED){
								Intent scannerIntent=createIntent(0, false, false);
								Runnable r=new Runnable(){
									@Override
									public void run(){
										if(scannerIntent.resolveActivity(context.getPackageManager())!=null){
											progress.dismiss();
											onSuccess.run();
										}else{
											UiUtils.runOnUiThread(this, 100);
										}
									}
								};
								UiUtils.runOnUiThread(r);
								GmsClient.disconnectFromService(context, connectionID);
							}else if(statusUpdate.installState==ModuleInstallStatusUpdate.STATE_FAILED || statusUpdate.installState==ModuleInstallStatusUpdate.STATE_CANCELED){
								UiUtils.runOnUiThread(()->{
									progress.dismiss();
									Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
								});
								GmsClient.disconnectFromService(context, connectionID);
							}
						}
					});
				}catch(RemoteException e){
					Log.e(TAG, "onSuccess: ", e);
					UiUtils.runOnUiThread(()->{
						progress.dismiss();
						Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
					});
					GmsClient.disconnectFromService(context, connectionID);
				}
			}

			@Override
			public void onError(Exception error){
				Log.e(TAG, "onError() called with: error = ["+error+"]");
				Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
				progress.dismiss();
			}
		});
	}
}
