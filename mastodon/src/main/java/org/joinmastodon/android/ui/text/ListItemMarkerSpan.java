package org.joinmastodon.android.ui.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

import me.grishka.appkit.utils.V;

public class ListItemMarkerSpan implements LeadingMarginSpan{
	public String text;

	public ListItemMarkerSpan(String text){
		this.text=text;
	}

	@Override
	public int getLeadingMargin(boolean first){
		return V.dp(32);
	}

	@Override
	public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout){
		if(text instanceof Spanned s && s.getSpanStart(this)==start){
			int level=s.getSpans(start, end, LeadingMarginSpan.class).length-1;
			if(dir<0){ // RTL
				c.drawText(this.text, layout.getWidth()-V.dp(32*level)-p.measureText(this.text), baseline, p);
			}else{
				c.drawText(this.text, x+V.dp(32*level), baseline, p);
			}
		}
	}
}
