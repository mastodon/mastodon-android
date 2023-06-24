package org.joinmastodon.android.ui.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.model.Emoji;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CustomEmojiSpan extends ReplacementSpan{
	public final Emoji emoji;
	private Drawable drawable;

	public CustomEmojiSpan(Emoji emoji){
		this.emoji=emoji;
	}

	@Override
	public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm){
		return Math.round(paint.descent()-paint.ascent());
	}

	@Override
	public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint){
		int size=Math.round(paint.descent()-paint.ascent());
		if(drawable==null){
			int alpha=paint.getAlpha();
			paint.setAlpha(alpha >> 1);
			canvas.drawRoundRect(x, top, x+size, top+size, V.dp(2), V.dp(2), paint);
			paint.setAlpha(alpha);
		}else{
			// AnimatedImageDrawable doesn't like when its bounds don't start at (0, 0)
			Rect bounds=drawable.getBounds();
			int dw=drawable.getIntrinsicWidth();
			int dh=drawable.getIntrinsicHeight();
			if(bounds.left!=0 || bounds.top!=0 || bounds.right!=dw || bounds.left!=dh){
				drawable.setBounds(0, 0, dw, dh);
			}
			canvas.save();
			canvas.translate(x, top);
			canvas.scale(size/(float)dw, size/(float)dh, 0f, 0f);
			drawable.draw(canvas);
			canvas.restore();
		}
	}

	public void setDrawable(Drawable drawable){
		this.drawable=drawable;
	}

	public UrlImageLoaderRequest createImageLoaderRequest(){
		int size=V.dp(20);
		return new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? emoji.url : emoji.staticUrl, size, size);
	}
}
