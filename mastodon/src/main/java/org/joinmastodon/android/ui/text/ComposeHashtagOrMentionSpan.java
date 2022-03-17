package org.joinmastodon.android.ui.text;

import android.graphics.Typeface;
import android.text.TextPaint;

public class ComposeHashtagOrMentionSpan extends ComposeAutocompleteSpan{
	private static final Typeface MEDIUM_TYPEFACE=Typeface.create("sans-serif-medium", 0);

	@Override
	public void updateDrawState(TextPaint tp){
		tp.setColor(tp.linkColor);
		tp.setTypeface(MEDIUM_TYPEFACE);
	}
}
