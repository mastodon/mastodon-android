package org.joinmastodon.android.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;

public class SpinnerListItemsAdapter<T> extends BaseAdapter implements SpinnerAdapter{
	public final GenericListItemsAdapter<T> actualAdapter;

	public SpinnerListItemsAdapter(GenericListItemsAdapter<T> actualAdapter){
		this.actualAdapter=actualAdapter;
	}

	@Override
	public int getCount(){
		return actualAdapter.getItemCount();
	}

	@Override
	public int getItemViewType(int position){
		return actualAdapter.getItemViewType(position);
	}

	@Override
	public ListItem<T> getItem(int position){
		return actualAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position){
		return 0;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent){
		ListItemViewHolder<?> holder;
		if(convertView==null){
			holder=actualAdapter.createViewHolder(parent, getItemViewType(position));
			holder.itemView.setTag(R.id.list_item_view_holder, holder);
		}else{
			holder=(ListItemViewHolder<?>) convertView.getTag(R.id.list_item_view_holder);
		}

		actualAdapter.onBindViewHolder(holder, position);

		return holder.itemView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		View view;
		if(convertView==null){
			view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spinner, parent, false);
		}else{
			view=convertView;
		}

		ListItem<T> item=getItem(position);
		if(item.titleRes!=0)
			((TextView)view).setText(item.titleRes);
		else
			((TextView)view).setText(item.title);

		return view;
	}
}
