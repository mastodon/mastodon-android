package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

public class CheckIconSelectableTextView extends TextView{

	private boolean currentlySelected;

	public CheckIconSelectableTextView(Context context){
		this(context, null);
	}

	public CheckIconSelectableTextView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CheckIconSelectableTextView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void drawableStateChanged(){
		super.drawableStateChanged();
		if(currentlySelected==isSelected())
			return;
		currentlySelected=isSelected();
		Drawable start=currentlySelected ? getResources().getDrawable(R.drawable.ic_baseline_check_18, getContext().getTheme()).mutate() : null;
		if(start!=null)
			start.setTint(UiUtils.getThemeColor(getContext(), R.attr.colorM3OnSurface));
		Drawable end=getCompoundDrawablesRelative()[2];
		setCompoundDrawablesRelativeWithIntrinsicBounds(start, null, end, null);
	}
}
