package org.joinmastodon.android.ui.adapters;

import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.viewholders.CheckboxOrRadioListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SwitchListItemViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GenericListItemsAdapter<T> extends RecyclerView.Adapter<ListItemViewHolder<?>>{
	private List<ListItem<T>> items;

	public GenericListItemsAdapter(List<ListItem<T>> items){
		this.items=items;
	}

	@NonNull
	@Override
	public ListItemViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		if(viewType==R.id.list_item_simple || viewType==R.id.list_item_simple_tinted)
			return new SimpleListItemViewHolder(parent.getContext(), parent);
		if(viewType==R.id.list_item_switch)
			return new SwitchListItemViewHolder(parent.getContext(), parent);
		if(viewType==R.id.list_item_checkbox)
			return new CheckboxOrRadioListItemViewHolder(parent.getContext(), parent, false);
		if(viewType==R.id.list_item_radio)
			return new CheckboxOrRadioListItemViewHolder(parent.getContext(), parent, true);

		throw new IllegalArgumentException("Unexpected view type "+viewType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBindViewHolder(@NonNull ListItemViewHolder<?> holder, int position){
		((ListItemViewHolder<ListItem<T>>)holder).bind(items.get(position));
	}

	@Override
	public int getItemCount(){
		return items.size();
	}

	@Override
	public int getItemViewType(int position){
		return items.get(position).getItemViewType();
	}
}
