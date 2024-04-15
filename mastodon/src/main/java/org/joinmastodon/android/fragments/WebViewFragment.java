package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.joinmastodon.android.api.MastodonErrorResponse;

import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;

public abstract class WebViewFragment extends LoaderFragment implements OnBackPressedListener{
	protected WebView webView;

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
				dataLoaded();
			}

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
				onError(new MastodonErrorResponse(error.getDescription().toString(), -1, null));
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
				return WebViewFragment.this.shouldOverrideUrlLoading(request);
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
	public boolean onBackPressed(){
		if(webView.canGoBack()){
			webView.goBack();
			return true;
		}
		return false;
	}

	@Override
	public void onToolbarNavigationClick(){
		Nav.finish(this);
	}

	protected abstract boolean shouldOverrideUrlLoading(WebResourceRequest req);
}
