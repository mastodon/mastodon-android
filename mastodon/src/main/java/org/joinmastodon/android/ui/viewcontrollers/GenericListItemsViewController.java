package org.joinmastodon.android.ui.viewcontrollers;

import android.content.Context;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.viewholders.CheckableListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;

import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.views.UsableRecyclerView;

public class GenericListItemsViewController<T>{
	private UsableRecyclerView list;
	private List<ListItem<T>> items;
	private GenericListItemsAdapter<T> adapter;
	private Context context;

	public GenericListItemsViewController(Context context, List<ListItem<T>> items){
		this.context=context;
		setItems(items);
	}

	public GenericListItemsViewController(Context context){
		this.context=context;
	}

	public void setItems(List<ListItem<T>> items){
		if(this.items!=null)
			throw new IllegalStateException("items already set");
		this.items=items;
		adapter=new GenericListItemsAdapter<>(items);
		list=new UsableRecyclerView(context);
		list.setLayoutManager(new LinearLayoutManager(context));
		list.setAdapter(adapter);
		list.addItemDecoration(new DividerItemDecoration(context, R.attr.colorM3OutlineVariant, 1, 16, 16, vh->(vh instanceof SimpleListItemViewHolder ivh && ivh.getItem().dividerAfter) || (vh instanceof CheckableListItemViewHolder cvh && cvh.getItem().dividerAfter)));
		list.setItemAnimator(new BetterItemAnimator());
	}

	public GenericListItemsAdapter<T> getAdapter(){
		return adapter;
	}

	public View getView(){
		return list;
	}

	public void rebindItem(ListItem<?> item){
		if(list.findViewHolderForAdapterPosition(items.indexOf(item)) instanceof ListItemViewHolder<?> holder){
			holder.rebind();
		}
	}
}
