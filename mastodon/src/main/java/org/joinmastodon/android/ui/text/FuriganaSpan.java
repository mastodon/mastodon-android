package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class FuriganaSpan extends ReplacementSpan{

	private static final String TAG = "FuriganaDebug";

	private final String furigana;
	private final float furiganaScale;

	private float textWidth;
	private Integer textColor = null;
	private boolean underline = false;
	private boolean bold = false;

	public FuriganaSpan(String furigana, float furiganaScale) {
		this.furigana = furigana;
		this.furiganaScale = furiganaScale;
	}

	public void captureVisualsFrom(Spanned sp, int start, int end, @Nullable Context context) {
		try {
			if (sp == null) return;

			ForegroundColorSpan[] colors = sp.getSpans(start, end, ForegroundColorSpan.class);
			if (colors.length > 0) {
				textColor = colors[colors.length - 1].getForegroundColor();
			}

			UnderlineSpan[] underlines = sp.getSpans(start, end, UnderlineSpan.class);
			if (underlines.length > 0) {
				underline = true;
			}

			StyleSpan[] styles = sp.getSpans(start, end, StyleSpan.class);
			if (styles.length > 0) {
				for (StyleSpan ss : styles) {
					if (ss.getStyle() == android.graphics.Typeface.BOLD) {
						bold = true;
						break;
					}
				}
			}

			if (textColor == null) {
				LinkSpan[] links =
						sp.getSpans(start, end, LinkSpan.class);
				if (links.length > 0) {
					if (context != null) {
						TypedValue tv = new TypedValue();
						Resources.Theme theme = context.getTheme();
						if (theme.resolveAttribute(android.R.attr.textColorLink, tv, true)) {
							if (tv.data != 0) {
								textColor = tv.data;
							} else if (tv.resourceId != 0) {
								textColor = context.getColor(tv.resourceId);
							}
						} else {
							// fallback
							textColor = 0xFF2196F3;
						}
					} else {
						textColor = 0xFF2196F3;
					}

					underline = true;
				}
			}

		} catch (Exception e) {
			Log.e(TAG, "Error in captureVisualsFrom()", e);
		}
	}

	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
		textWidth = paint.measureText(text, start, end);

		if (fm != null) {
			int extra = (int) (paint.getTextSize() * furiganaScale * 1.2f);
			extra += V.dp(2);

			fm.ascent -= extra;
			fm.top -= extra;
		}

		Paint fPaint = new Paint(paint);
		fPaint.setTextSize(paint.getTextSize() * furiganaScale);
		float furiganaWidth = fPaint.measureText(furigana);
		float width = Math.max(textWidth, furiganaWidth);

		return (int) width;
	}

	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end,
					 float x, int top, int y, int bottom, Paint paint) {
		String main = text.subSequence(start, end).toString();

		float baseTextSize = paint.getTextSize();

		Paint fPaint = new Paint(paint);
		fPaint.setTextSize(baseTextSize * furiganaScale);

		float furiganaWidth = fPaint.measureText(furigana);
		float mainWidth = paint.measureText(main);

		float furiganaX = x + (Math.max(mainWidth, furiganaWidth) - furiganaWidth) / 2f;
		float furiganaY = y - baseTextSize * 1f;

		if (textColor != null) {
			fPaint.setColor(textColor);
		}

		canvas.save();
		canvas.drawText(furigana, furiganaX, furiganaY, fPaint);
		canvas.restore();

		Paint mainPaint = new Paint(paint);
		if (textColor != null) {
			mainPaint.setColor(textColor);
		}
		if (bold) {
			mainPaint.setFakeBoldText(true);
		}

		float mainX = x + (Math.max(mainWidth, furiganaWidth) - mainWidth) / 2f;
		canvas.drawText(main, mainX, y, mainPaint);

		if (underline) {
			float underlineY = y + mainPaint.getTextSize() * 0.1f;
			canvas.drawLine(mainX, underlineY, mainX + mainWidth, underlineY, mainPaint);
		}
	}
}
