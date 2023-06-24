package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

public class LengthLimitHighlighter implements TextWatcher{
	private final Context context;
	private final int lengthLimit;
	private BackgroundColorSpan overLimitBG;
	private ForegroundColorSpan overLimitFG;
	private boolean isOverLimit;
	private OverLimitChangeListener listener;

	public LengthLimitHighlighter(Context context, int lengthLimit){
		this.context=context;
		overLimitBG=new BackgroundColorSpan(UiUtils.getThemeColor(context, R.attr.colorM3ErrorContainer));
		overLimitFG=new ForegroundColorSpan(UiUtils.getThemeColor(context, R.attr.colorM3Error));
		this.lengthLimit=lengthLimit;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after){

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count){

	}

	@Override
	public void afterTextChanged(Editable s){
		s.removeSpan(overLimitBG);
		s.removeSpan(overLimitFG);
		boolean newOverLimit=s.length()>lengthLimit;
		if(newOverLimit){
			int start=s.length()-(s.length()-lengthLimit);
			int end=s.length();
			s.setSpan(overLimitFG, start, end, 0);
			s.setSpan(overLimitBG, start, end, 0);
		}
		if(newOverLimit!=isOverLimit){
			isOverLimit=newOverLimit;
			if(listener!=null)
				listener.onOverLimitChanged(isOverLimit);
		}
	}

	public LengthLimitHighlighter setListener(OverLimitChangeListener listener){
		this.listener=listener;
		return this;
	}

	public boolean isOverLimit(){
		return isOverLimit;
	}

	public interface OverLimitChangeListener{
		void onOverLimitChanged(boolean isOverLimit);
	}
}
