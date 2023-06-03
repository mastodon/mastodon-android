package org.joinmastodon.android.ui.adapters;

import android.view.ViewGroup;

import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.viewholders.InstanceRuleViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class InstanceRulesAdapter extends RecyclerView.Adapter<InstanceRuleViewHolder>{
	private final List<Instance.Rule> rules;

	public InstanceRulesAdapter(List<Instance.Rule> rules){
		this.rules=rules;
	}

	@NonNull
	@Override
	public InstanceRuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		return new InstanceRuleViewHolder(parent);
	}

	@Override
	public void onBindViewHolder(@NonNull InstanceRuleViewHolder holder, int position){
		holder.setPosition(position);
		holder.bind(rules.get(position));
	}

	@Override
	public int getItemCount(){
		return rules.size();
	}
}
