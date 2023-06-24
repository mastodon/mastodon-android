package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import me.grishka.appkit.utils.V;

public class CheckboxOrRadioListItemViewHolder extends CheckableListItemViewHolder{
	public CheckboxOrRadioListItemViewHolder(Context context, ViewGroup parent, boolean radio){
		super(context, parent);
		View iconView=new View(context);
		iconView.setDuplicateParentStateEnabled(true);
		CompoundButton terribleHack=radio ? new RadioButton(context) : new CheckBox(context);
		iconView.setBackground(terribleHack.getButtonDrawable());
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(V.dp(32), V.dp(32));
		lp.setMarginStart(V.dp(12));
		lp.setMarginEnd(V.dp(4));
		checkableLayout.addView(iconView, lp);
	}
}
