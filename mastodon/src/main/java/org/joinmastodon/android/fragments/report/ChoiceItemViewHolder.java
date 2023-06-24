package org.joinmastodon.android.fragments.report;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.views.CheckableLinearLayout;

import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

class ChoiceItemViewHolder extends BindableViewHolder<ChoiceItem> implements UsableRecyclerView.Clickable{
	private final TextView title, subtitle;
	private final View checkbox;
	private final CheckableLinearLayout view;
	private final boolean isMultipleChoice;
	private final RecyclerView list;
	private final ArrayList<String> selectedIDs;
	private final Consumer<Boolean> buttonEnabledSetter;

	public ChoiceItemViewHolder(Context context, boolean isMultipleChoice, RecyclerView list, ArrayList<String> selectedIDs, Consumer<Boolean> buttonEnabledSetter){
		super(context, R.layout.item_report_choice, list);
		this.buttonEnabledSetter=buttonEnabledSetter;
		this.isMultipleChoice=isMultipleChoice;
		this.list=list;
		this.selectedIDs=selectedIDs;
		title=findViewById(R.id.title);
		subtitle=findViewById(R.id.subtitle);
		checkbox=findViewById(R.id.checkbox);
		CompoundButton cb=isMultipleChoice ? new CheckBox(context) : new RadioButton(context);
		checkbox.setBackground(cb.getButtonDrawable());
		view=(CheckableLinearLayout) itemView;
	}

	@Override
	public void onBind(ChoiceItem item){
		title.setText(item.title);
		if(TextUtils.isEmpty(item.subtitle)){
			subtitle.setVisibility(View.GONE);
			view.setMinimumHeight(V.dp(56));
		}else{
			subtitle.setVisibility(View.VISIBLE);
			subtitle.setText(item.subtitle);
			view.setMinimumHeight(V.dp(72));
		}
		view.setChecked(selectedIDs.contains(item.id));
	}

	@Override
	public void onClick(){
		if(isMultipleChoice){
			if(selectedIDs.contains(item.id))
				selectedIDs.remove(item.id);
			else
				selectedIDs.add(item.id);
			rebind();
		}else{
			if(!selectedIDs.contains(item.id)){
				if(!selectedIDs.isEmpty()){
					String prev=selectedIDs.remove(0);
					for(int i=0; i<list.getChildCount(); i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder instanceof ChoiceItemViewHolder ivh && ivh.getItem().id.equals(prev)){
							ivh.rebind();
							break;
						}
					}
				}
				selectedIDs.add(item.id);
				rebind();
			}
		}
		buttonEnabledSetter.accept(!selectedIDs.isEmpty());
	}
}
