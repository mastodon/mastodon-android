package org.joinmastodon.android.ui.utils;

import android.content.Context;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;

public class UiUtils{
	private UiUtils(){}

	public static void launchWebBrowser(Context context, String url){
		// TODO setting for custom tabs
		new CustomTabsIntent.Builder()
				.build()
				.launchUrl(context, Uri.parse(url));
	}
}
