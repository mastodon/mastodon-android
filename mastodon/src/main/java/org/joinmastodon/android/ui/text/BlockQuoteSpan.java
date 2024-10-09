package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.V;

public class BlockQuoteSpan extends CharacterStyle implements LeadingMarginSpan{
	private final Context context;
	private Drawable icon;
	private boolean firstLevel;
	private Paint paint=new Paint();

	public BlockQuoteSpan(Context context, boolean firstLevel){
		this.context=context;
		icon=context.getResources().getDrawable(R.drawable.quote, context.getTheme());
		this.firstLevel=firstLevel;
		paint.setColor(UiUtils.getThemeColor(context, R.attr.colorM3TertiaryContainer));
		paint.setAlpha(51);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(V.dp(3));
	}


	@Override
	public int getLeadingMargin(boolean first){
		return V.dp(firstLevel ? 32 : 18);
	}

	@Override
	public void drawLeadingMargin(@NonNull Canvas c, @NonNull Paint p, int x, int dir, int top, int baseline, int bottom, @NonNull CharSequence text, int start, int end, boolean first, @NonNull Layout layout){
		if(text instanceof Spanned s && s.getSpanStart(this)==start){
			int level=s.getSpans(start, end, LeadingMarginSpan.class).length-1;
			if(dir<0){ // RTL
//				c.drawText(this.text, layout.getWidth()-V.dp(32*level)-p.measureText(this.text), baseline, p);
				if(level==0){
					icon.setBounds(layout.getWidth()-icon.getIntrinsicWidth(), top, layout.getWidth(), top+icon.getIntrinsicHeight());
					icon.draw(c);
				}else{
					float xOffset=layout.getWidth()-V.dp(32+18*(level-1)+1.5f);
					c.drawLine(xOffset, top, xOffset, layout.getLineBottom(layout.getLineForOffset(s.getSpanEnd(this))), paint);
				}
			}else{
				if(level==0){
					icon.setBounds(x, top, x+icon.getIntrinsicWidth(), top+icon.getIntrinsicHeight());
					icon.draw(c);
				}else{
					float xOffset=x+V.dp(32+18*(level-1)+1.5f);
					c.drawLine(xOffset, top, xOffset, layout.getLineBottom(layout.getLineForOffset(s.getSpanEnd(this))), paint);
				}
			}
		}
	}

	@Override
	public void updateDrawState(TextPaint tp){
		tp.setColor(UiUtils.getThemeColor(context, R.attr.colorM3Tertiary));
	}
}
