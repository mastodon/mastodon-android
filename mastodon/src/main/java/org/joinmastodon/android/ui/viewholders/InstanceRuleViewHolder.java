package org.joinmastodon.android.ui.viewholders;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.text.HtmlParser;

import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceRuleViewHolder extends BindableViewHolder<Instance.Rule> implements UsableRecyclerView.DisableableClickable{
	private final TextView text, number, hint;
	private int position;

	public InstanceRuleViewHolder(ViewGroup parent){
		super(parent.getContext(), R.layout.item_server_rule, parent);
		text=findViewById(R.id.text);
		number=findViewById(R.id.number);
		hint=findViewById(R.id.hint);
	}

	public void setPosition(int position){
		this.position=position;
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onBind(Instance.Rule item){
		if(item.parsedText==null){
			item.parsedText=HtmlParser.parseLinks(item.getTranslatedText());
			String hint=item.getTranslatedHint();
			if(!TextUtils.isEmpty(hint))
				item.parsedHint=HtmlParser.parseLinks(hint);
		}
		text.setText(item.parsedText);
		number.setText(String.format("%d", position+1));
		if(item.parsedHint==null){
			hint.setVisibility(View.GONE);
		}else{
			hint.setVisibility(View.VISIBLE);
			hint.setText(item.parsedHint);
		}
		hint.setMaxLines(item.hintExpanded ? Integer.MAX_VALUE : 2);
	}

	@Override
	public boolean isEnabled(){
		return hint.getVisibility()==View.VISIBLE;
	}

	@Override
	public void onClick(){
		item.hintExpanded=!item.hintExpanded;
		hint.setMaxLines(item.hintExpanded ? Integer.MAX_VALUE : 2);
	}
}
