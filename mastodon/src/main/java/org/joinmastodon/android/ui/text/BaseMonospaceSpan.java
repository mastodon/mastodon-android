package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.ColorContrastMode;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.V;

public abstract class BaseMonospaceSpan extends TypefaceSpan{
	private final Context context;

	public BaseMonospaceSpan(Context context){
		super("monospace");
		this.context=context;
	}

	@Override
	public void updateDrawState(@NonNull TextPaint paint){
		super.updateDrawState(paint);
		if(!UiUtils.isDarkTheme() && UiUtils.getColorContrastMode(context)==ColorContrastMode.HIGH){

		}else{
			paint.setColor(UiUtils.getThemeColor(context, R.attr.colorRichTextText));
		}
		paint.setTextSize(paint.getTextSize()*0.9375f);
		paint.baselineShift=V.dp(-1);
	}

	@Override
	public void updateMeasureState(@NonNull TextPaint paint){
		super.updateMeasureState(paint);
		paint.setTextSize(paint.getTextSize()*0.9375f);
		paint.baselineShift=V.dp(-1);
	}
}
