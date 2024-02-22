package org.joinmastodon.android.googleservices;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService;

import java.util.function.Function;

public class GmsClient{
	private static final String TAG="GmsClient";
	private static final SparseArray<ServiceConnection> currentConnections=new SparseArray<>();
	private static int nextConnectionID=0;

	public static <I extends IInterface> void connectToService(Context context, String action, int id, boolean useDynamicLookup, ServiceConnectionCallback<I> callback, Function<IBinder, I> asInterface){
		Intent intent;
		if(useDynamicLookup){
			try{
				Bundle args=new Bundle();
				args.putString("serviceActionBundleKey", action);
				Bundle result=context.getContentResolver().call(new Uri.Builder().scheme("content").authority("com.google.android.gms.chimera").build(), "serviceIntentCall", null, args);
				if(result==null)
					throw new IllegalStateException("Dynamic lookup failed");
				intent=result.getParcelable("serviceResponseIntentKey");
				if(intent==null)
					throw new IllegalStateException("Dynamic lookup returned null");
			}catch(Exception x){
				callback.onError(x);
				return;
			}
		}else{
			intent=new Intent(action);
		}
		intent.setPackage("com.google.android.gms");
		ServiceConnection conn=new ServiceConnection(){
			@Override
			public void onServiceConnected(ComponentName name, IBinder service){
				IGmsServiceBroker broker=IGmsServiceBroker.Stub.asInterface(service);
				GetServiceRequest req=new GetServiceRequest();
				req.serviceId=id;
				req.packageName=context.getPackageName();
				ServiceConnection serviceConnectionThis=this;
				try{
					broker.getService(new IGmsCallbacks.Stub(){
						@Override
						public void onPostInitComplete(int statusCode, IBinder binder, Bundle params) throws RemoteException{
							int connectionID=nextConnectionID++;
							currentConnections.put(connectionID, serviceConnectionThis);
							callback.onSuccess(asInterface.apply(binder), connectionID);
						}

						@Override
						public void onAccountValidationComplete(int statusCode, Bundle params) throws RemoteException{}

						@Override
						public void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder, ConnectionInfo info) throws RemoteException{
							onPostInitComplete(statusCode, binder, info!=null ? info.params : null);
						}
					}, req);
				}catch(Exception x){
					callback.onError(x);
					context.unbindService(this);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name){}
		};
		boolean res=context.bindService(intent, conn, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND | Context.BIND_ADJUST_WITH_ACTIVITY);
		if(!res){
			context.unbindService(conn);
			callback.onError(new IllegalStateException("Service connection failed"));
		}
	}

	public static void disconnectFromService(Context context, int connectionID){
		ServiceConnection conn=currentConnections.get(connectionID);
		if(conn!=null){
			currentConnections.remove(connectionID);
			context.unbindService(conn);
		}
	}

	public static boolean isGooglePlayServicesAvailable(Context context){
		PackageManager pm=context.getPackageManager();
		try{
			pm.getPackageInfo("com.google.android.gms", 0);
			return true;
		}catch(PackageManager.NameNotFoundException e){
			return false;
		}
	}

	public static void getModuleInstallerService(Context context, ServiceConnectionCallback<IModuleInstallService> callback){
		connectToService(context, "com.google.android.gms.chimera.container.moduleinstall.ModuleInstallService.START", 308, true, callback, IModuleInstallService.Stub::asInterface);
	}

	public interface ServiceConnectionCallback<I extends IInterface>{
		void onSuccess(I service, int connectionID);
		void onError(Exception error);
	}
}
