package org.joinmastodon.android.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;

import java.util.Objects;

public class EphemeralWebViewFragment extends WebViewFragment{

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		removeCookies(); // In case the app was perviously terminated with this fragment active
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
			webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
		}
		webView.loadUrl(Objects.requireNonNull(getArguments().getString("url")));
	}

	@Override
	protected boolean shouldOverrideUrlLoading(WebResourceRequest req){
		return false;
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		removeCookies();
	}

	private void removeCookies(){
		CookieManager cm=CookieManager.getInstance();
		cm.removeAllCookies(null);
	}
}
