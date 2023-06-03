package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.ui.views.M3Switch;

import me.grishka.appkit.utils.V;

public class SwitchListItemViewHolder extends CheckableListItemViewHolder{
	private final M3Switch sw;
	private boolean ignoreListener;

	public SwitchListItemViewHolder(Context context, ViewGroup parent){
		super(context, parent);
		sw=new M3Switch(context);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(V.dp(52), V.dp(32));
		lp.gravity=Gravity.TOP;
		lp.setMarginStart(V.dp(16));
		checkableLayout.addView(sw, lp);
		sw.setOnCheckedChangeListener((buttonView, isChecked)->{
			if(ignoreListener)
				return;
			if(item.checkedChangeListener!=null)
				item.checkedChangeListener.accept(isChecked);
			else
				item.checked=isChecked;
		});
		sw.setClickable(true);
	}

	@Override
	public void onBind(CheckableListItem<?> item){
		super.onBind(item);
		ignoreListener=true;
		sw.setChecked(item.checked);
		sw.setEnabled(item.isEnabled);
		ignoreListener=false;
	}
}
