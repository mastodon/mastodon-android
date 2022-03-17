package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class SelectionListenerEditText extends EditText{
	private SelectionListener selectionListener;

	public SelectionListenerEditText(Context context){
		super(context);
	}

	public SelectionListenerEditText(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public SelectionListenerEditText(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	public SelectionListenerEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd){
		super.onSelectionChanged(selStart, selEnd);
		if(selectionListener!=null)
			selectionListener.onSelectionChanged(selStart, selEnd);
	}

	public void setSelectionListener(SelectionListener selectionListener){
		this.selectionListener=selectionListener;
	}

	public interface SelectionListener{
		void onSelectionChanged(int start, int end);
	}
}
