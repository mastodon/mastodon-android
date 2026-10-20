package org.joinmastodon.android.ui.text;

import android.text.TextPaint;

public class NonColoredUnderlinedLinkSpan extends NonColoredLinkSpan{
	public NonColoredUnderlinedLinkSpan(String link, OnLinkClickListener listener, Type type, String accountID, Object linkObject, Object parentObject){
		super(link, listener, type, accountID, linkObject, parentObject);
	}

	public NonColoredUnderlinedLinkSpan(LinkSpan ls){
		super(ls);
	}

	@Override
	public void updateDrawState(TextPaint tp){
		super.updateDrawState(tp);
		tp.setUnderlineText(true);
	}
}
