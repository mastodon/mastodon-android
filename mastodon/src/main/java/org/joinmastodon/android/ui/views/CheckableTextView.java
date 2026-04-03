package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.TextView;

public class CheckableTextView extends TextView implements Checkable{
	private boolean checked, checkable=true;
	private static final int[] CHECKED_STATE_SET = {
			android.R.attr.state_checked
	};

	public CheckableTextView(Context context){
		this(context, null);
	}

	public CheckableTextView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CheckableTextView(Context context, AttributeSet attrs, int defStyle){
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

	public void setCheckable(boolean checkable){
		this.checkable=checkable;
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
		info.setCheckable(checkable);
		info.setChecked(checked);
	}
}
