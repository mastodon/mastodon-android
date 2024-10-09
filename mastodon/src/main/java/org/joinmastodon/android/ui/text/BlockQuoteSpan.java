package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
		icon=context.getResources().getDrawable(R.drawable.quote, context.getTheme()).mutate();
		this.firstLevel=firstLevel;
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
			int color;
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && UiUtils.isDarkTheme()){
				color=UiUtils.alphaBlendColors(
						context.getColor(android.R.color.system_accent3_700),
						context.getColor(android.R.color.system_accent3_800),
						0.5f
				);
			}else{
				color=UiUtils.getThemeColor(context, R.attr.colorRichTextDecorations);
			}
			int level=s.getSpans(start, end, LeadingMarginSpan.class).length-1;
			if(dir<0){ // RTL
				if(level==0){
					icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
					icon.setBounds(layout.getWidth()-icon.getIntrinsicWidth(), top, layout.getWidth(), top+icon.getIntrinsicHeight());
					icon.draw(c);
				}else{
					paint.setColor(color);
					float xOffset=layout.getWidth()-V.dp(32+18*(level-1)+1.5f);
					c.drawLine(xOffset, top, xOffset, layout.getLineBottom(layout.getLineForOffset(s.getSpanEnd(this))), paint);
				}
			}else{
				if(level==0){
					icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
					icon.setBounds(x, top, x+icon.getIntrinsicWidth(), top+icon.getIntrinsicHeight());
					icon.draw(c);
				}else{
					paint.setColor(color);
					float xOffset=x+V.dp(32+18*(level-1)+1.5f);
					c.drawLine(xOffset, top, xOffset, layout.getLineBottom(layout.getLineForOffset(s.getSpanEnd(this))), paint);
				}
			}
		}
	}

	@Override
	public void updateDrawState(TextPaint tp){
		tp.setColor(UiUtils.getThemeColor(context, R.attr.colorRichTextText));
	}
}
