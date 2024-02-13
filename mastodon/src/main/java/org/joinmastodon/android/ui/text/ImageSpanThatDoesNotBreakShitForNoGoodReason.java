package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageSpanThatDoesNotBreakShitForNoGoodReason extends ImageSpan{
	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Bitmap b){
		super(b);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Bitmap b, int verticalAlignment){
		super(b, verticalAlignment);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, @NonNull Bitmap bitmap){
		super(context, bitmap);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, @NonNull Bitmap bitmap, int verticalAlignment){
		super(context, bitmap, verticalAlignment);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Drawable drawable){
		super(drawable);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Drawable drawable, int verticalAlignment){
		super(drawable, verticalAlignment);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Drawable drawable, @NonNull String source){
		super(drawable, source);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Drawable drawable, @NonNull String source, int verticalAlignment){
		super(drawable, source, verticalAlignment);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, @NonNull Uri uri){
		super(context, uri);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, @NonNull Uri uri, int verticalAlignment){
		super(context, uri, verticalAlignment);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, int resourceId){
		super(context, resourceId);
	}

	public ImageSpanThatDoesNotBreakShitForNoGoodReason(@NonNull Context context, int resourceId, int verticalAlignment){
		super(context, resourceId, verticalAlignment);
	}

	@Override
	public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm){
		// Purposefully not touching the font metrics
		return getDrawable().getBounds().right;
	}
}
