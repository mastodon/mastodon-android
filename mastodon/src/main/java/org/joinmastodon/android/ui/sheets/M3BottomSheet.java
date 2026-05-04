package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.views.BottomSheet;

public class M3BottomSheet extends BottomSheet{
	public M3BottomSheet(@NonNull Context context){
		super(context);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());
	}
}
