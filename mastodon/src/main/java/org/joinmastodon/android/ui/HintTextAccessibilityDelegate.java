package org.joinmastodon.android.ui;

import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

public class HintTextAccessibilityDelegate extends View.AccessibilityDelegate{
	private final int res;

	public HintTextAccessibilityDelegate(int res){
		this.res=res;
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info){
		super.onInitializeAccessibilityNodeInfo(host, info);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			info.setHintText(host.getContext().getString(res));
		}
	}
}
