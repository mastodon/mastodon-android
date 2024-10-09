package org.joinmastodon.android.ui.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpacerSpan extends ReplacementSpan{
	private int width, height;

	public SpacerSpan(int width, int height){
		this.width=width;
		this.height=height;
	}

	@Override
	public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm){
		if(fm!=null && height>0){
			fm.ascent=-height;
			fm.descent=0;
			fm.top=fm.ascent;
			fm.bottom=0;
		}
		return width;
	}

	@Override
	public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint){

	}
}
