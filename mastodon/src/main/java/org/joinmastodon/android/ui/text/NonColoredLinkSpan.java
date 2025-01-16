package org.joinmastodon.android.ui.text;

import android.text.TextPaint;

public class NonColoredLinkSpan extends LinkSpan{
	public NonColoredLinkSpan(String link, OnLinkClickListener listener, Type type, String accountID, Object linkObject, Object parentObject){
		super(link, listener, type, accountID, linkObject, parentObject);
	}

	@Override
	public void updateDrawState(TextPaint tp){
		color=tp.getColor();
	}
}
