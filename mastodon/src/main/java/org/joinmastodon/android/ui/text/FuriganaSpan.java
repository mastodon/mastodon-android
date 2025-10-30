package org.joinmastodon.android.ui.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

public class FuriganaSpan extends ReplacementSpan {
	private final String furigana;
	private final float furiganaScale;  // 0.5–0.8
	private float textWidth;

	public FuriganaSpan(String furigana, float furiganaScale) {
		this.furigana = furigana;
		this.furiganaScale = furiganaScale;
	}

	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		textWidth = paint.measureText(text, start, end);

		if (fm != null) {
			int extra = (int)(paint.getTextSize() * furiganaScale * 1.2f);
			fm.ascent -= extra;
			fm.top -= extra;
		}

		Paint fPaint = new Paint(paint);
		fPaint.setTextSize(paint.getTextSize() * furiganaScale);
		float furiganaWidth = fPaint.measureText(furigana);
		return (int)Math.max(textWidth, furiganaWidth);
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

		canvas.save();
		canvas.drawText(furigana, furiganaX, furiganaY, fPaint);
		canvas.restore();

		float mainX = x + (Math.max(mainWidth, furiganaWidth) - mainWidth) / 2f;
		canvas.drawText(main, mainX, y, paint);
	}

}
