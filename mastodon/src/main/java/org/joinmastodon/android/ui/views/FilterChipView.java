package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.DrawableRes;
import me.grishka.appkit.utils.V;

public class FilterChipView extends Button{

	private boolean currentlySelected;

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
		setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(context, R.attr.colorM3OnSurface)));
		updatePadding();
	}

	@Override
	protected void drawableStateChanged(){
		super.drawableStateChanged();
		if(currentlySelected==isSelected())
			return;
		currentlySelected=isSelected();
		Drawable start=currentlySelected ? getResources().getDrawable(R.drawable.ic_baseline_check_18, getContext().getTheme()) : null;
		Drawable end=getCompoundDrawablesRelative()[2];
		setCompoundDrawablesRelativeWithIntrinsicBounds(start, null, end, null);
		updatePadding();
	}

	private void updatePadding(){
		int vertical=V.dp(6);
		Drawable[] drawables=getCompoundDrawablesRelative();
		setPaddingRelative(V.dp(drawables[0]==null ? 16 : 8), vertical, V.dp(drawables[2]==null ? 16 : 8), vertical);
	}

	public void setDrawableEnd(@DrawableRes int drawable){
		setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawable, 0);
		updatePadding();
	}
}
