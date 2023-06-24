package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable{
	private boolean checked;
	private static final int[] CHECKED_STATE_SET = {
			android.R.attr.state_checked
	};

	public CheckableLinearLayout(Context context){
		this(context, null);
	}

	public CheckableLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	public void setChecked(boolean checked){
		this.checked=checked;
		refreshDrawableState();
	}

	@Override
	public boolean isChecked(){
		return checked;
	}

	@Override
	public void toggle(){
		setChecked(!checked);
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
		super.onInitializeAccessibilityNodeInfo(info);
		info.setCheckable(true);
		info.setChecked(checked);
	}
}
