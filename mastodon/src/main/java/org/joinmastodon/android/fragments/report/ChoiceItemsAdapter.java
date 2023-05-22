package org.joinmastodon.android.fragments.report;

import android.content.Context;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.views.UsableRecyclerView;

class ChoiceItemsAdapter extends RecyclerView.Adapter<ChoiceItemViewHolder>{

	private final Context context;
	private final boolean isMultipleChoice;
	private final ArrayList<ChoiceItem> items;
	private final RecyclerView list;
	private final ArrayList<String> selectedIDs;
	private final Consumer<Boolean> buttonEnabledSetter;

	public ChoiceItemsAdapter(Context context, boolean isMultipleChoice, ArrayList<ChoiceItem> items, RecyclerView list, ArrayList<String> selectedIDs, Consumer<Boolean> buttonEnabledSetter){
		this.context=context;
		this.isMultipleChoice=isMultipleChoice;
		this.items=items;
		this.list=list;
		this.selectedIDs=selectedIDs;
		this.buttonEnabledSetter=buttonEnabledSetter;
	}

	@NonNull
	@Override
	public ChoiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		return new ChoiceItemViewHolder(context, isMultipleChoice, list, selectedIDs, buttonEnabledSetter);
	}

	@Override
	public void onBindViewHolder(@NonNull ChoiceItemViewHolder holder, int position){
		holder.bind(items.get(position));
	}

	@Override
	public int getItemCount(){
		return items.size();
	}
}
