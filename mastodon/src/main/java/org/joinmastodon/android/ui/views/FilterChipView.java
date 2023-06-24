package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.DrawableRes;
import me.grishka.appkit.utils.V;

public class FilterChipView extends CheckIconSelectableTextView{

	public FilterChipView(Context context){
		this(context, null);
	}

	public FilterChipView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public FilterChipView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		setCompoundDrawablePadding(V.dp(8));
		setBackgroundResource(R.drawable.bg_filter_chip);
		setTextAppearance(R.style.m3_label_large);
		setTextColor(getResources().getColorStateList(R.color.filter_chip_text, context.getTheme()));
		updatePadding();
	}

	@Override
	protected void drawableStateChanged(){
		super.drawableStateChanged();
		updatePadding();
	}

	private void updatePadding(){
		int vertical=V.dp(6);
		Drawable[] drawables=getCompoundDrawablesRelative();
		setPaddingRelative(V.dp(drawables[0]==null ? 16 : 8), vertical, V.dp(drawables[2]==null ? 16 : 8), vertical);
	}

	public void setDrawableEnd(@DrawableRes int drawable){
		Drawable icon=getResources().getDrawable(drawable, getContext().getTheme()).mutate();
		icon.setBounds(0, 0, V.dp(18), V.dp(18));
		icon.setTint(UiUtils.getThemeColor(getContext(), R.attr.colorM3OnSurface));
		setCompoundDrawablesRelativeWithIntrinsicBounds(getCompoundDrawablesRelative()[0], null, icon, null);
		updatePadding();
	}

	public void setDrawableStartTinted(@DrawableRes int drawable){
		Drawable icon=getResources().getDrawable(drawable, getContext().getTheme()).mutate();
		icon.setBounds(0, 0, V.dp(18), V.dp(18));
		icon.setTint(UiUtils.getThemeColor(getContext(), R.attr.colorM3Primary));
		setCompoundDrawablesRelative(icon, null, getCompoundDrawablesRelative()[2], null);
		updatePadding();
	}
}
