package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.api.MastodonErrorResponse;

import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.LoaderFragment;

public abstract class WebViewFragment extends LoaderFragment{
	private static final String TAG="WebViewFragment";

	protected WebView webView;
	private Runnable backCallback=this::onGoBack;
	private boolean backCallbackSet;

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		webView=new WebView(getActivity());
		webView.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onReceivedTitle(WebView view, String title){
				setTitle(title);
			}
		});
		webView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageFinished(WebView view, String url){
				if(BuildConfig.DEBUG){
					Log.d(TAG, "onPageFinished: "+url);
				}
				dataLoaded();
				updateBackCallback();
			}

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
				if(!loaded){
					onError(new MastodonErrorResponse(error.getDescription().toString(), -1, null));
					updateBackCallback();
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
				return WebViewFragment.this.shouldOverrideUrlLoading(request);
			}

			@Override
			public void doUpdateVisitedHistory(WebView view, String url, boolean isReload){
				updateBackCallback();
			}
		});
		webView.getSettings().setJavaScriptEnabled(true);
		return webView;
	}

	@Override
	protected void doLoadData(){

	}

	@Override
	public void onRefresh(){
		webView.reload();
	}

	@Override
	public void onToolbarNavigationClick(){
		Nav.finish(this);
	}

	private void updateBackCallback(){
		boolean canGoBack=webView.canGoBack();
		if(canGoBack!=backCallbackSet){
			if(canGoBack){
				addBackCallback(backCallback);
				backCallbackSet=true;
			}else{
				removeBackCallback(backCallback);
				backCallbackSet=false;
			}
		}
	}

	private void onGoBack(){
		if(webView.canGoBack())
			webView.goBack();
	}

	protected abstract boolean shouldOverrideUrlLoading(WebResourceRequest req);
}
