package org.joinmastodon.android.api;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.MastodonApp;

import java.net.InetSocketAddress;
import java.net.Proxy;

import androidx.annotation.RequiresApi;
import okhttp3.Call;
import okhttp3.EventListener;

@RequiresApi(api = Build.VERSION_CODES.CINNAMON_BUN)
public class LocalNetworkPermissionEventListener extends EventListener{
	public static LocalNetworkPermissionEventListener instance;
	public MainActivity currentMainActivity;

	public static LocalNetworkPermissionEventListener getInstance(){
		if(instance==null){
			instance=new LocalNetworkPermissionEventListener();
		}
		return instance;
	}

	private LocalNetworkPermissionEventListener(){}

	@Override
	public void connectStart(Call call, InetSocketAddress addr, Proxy proxy){
		if(addr.getAddress().isSiteLocalAddress() && MastodonApp.context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)!=PackageManager.PERMISSION_GRANTED){
			if(currentMainActivity!=null){
				// TODO cancel and restart the request?
				// TODO handle permission denial by showing something in the UI?
				currentMainActivity.requestLocalNetworkPermission();
			}
		}
	}
}
