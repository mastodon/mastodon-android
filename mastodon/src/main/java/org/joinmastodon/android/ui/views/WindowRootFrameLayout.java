package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.WindowInsets;
import android.widget.FrameLayout;

public class WindowRootFrameLayout extends FrameLayout{
	private OnKeyListener dispatchKeyEventListener;
	private OnApplyWindowInsetsListener dispatchApplyWindowInsetsListener;

	public WindowRootFrameLayout(Context context){
		this(context, null);
	}

	public WindowRootFrameLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public WindowRootFrameLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event){
		return (dispatchKeyEventListener!=null && dispatchKeyEventListener.onKey(this, event.getKeyCode(), event)) || super.dispatchKeyEvent(event);
	}

	public void setDispatchKeyEventListener(OnKeyListener dispatchKeyEventListener){
		this.dispatchKeyEventListener=dispatchKeyEventListener;
	}

	@Override
	public WindowInsets dispatchApplyWindowInsets(WindowInsets insets){
		if(dispatchApplyWindowInsetsListener!=null)
			return dispatchApplyWindowInsetsListener.onApplyWindowInsets(this, insets);
		return super.dispatchApplyWindowInsets(insets);
	}

	public void setDispatchApplyWindowInsetsListener(OnApplyWindowInsetsListener dispatchApplyWindowInsetsListener){
		this.dispatchApplyWindowInsetsListener=dispatchApplyWindowInsetsListener;
	}
}
