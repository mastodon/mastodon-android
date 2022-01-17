package org.joinmastodon.android.fragments;

import android.view.ViewGroup;

import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class StatusListFragment extends BaseRecyclerFragment<Status>{
	protected ArrayList<StatusDisplayItem> displayItems=new ArrayList<>();

	public StatusListFragment(){
		super(20);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new DisplayItemsAdapter();
	}

	@Override
	public void onAppendItems(List<Status> items){
		super.onAppendItems(items);
		for(Status s:items){
			displayItems.addAll(StatusDisplayItem.buildItems(this, s));
		}
	}

	@Override
	public void onClearItems(){
		super.onClearItems();
		displayItems.clear();
	}

	protected class DisplayItemsAdapter extends UsableRecyclerView.Adapter<BindableViewHolder<StatusDisplayItem>> implements ImageLoaderRecyclerAdapter{

		public DisplayItemsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BindableViewHolder<StatusDisplayItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return (BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(StatusDisplayItem.Type.values()[viewType], getActivity(), parent);
		}

		@Override
		public void onBindViewHolder(BindableViewHolder<StatusDisplayItem> holder, int position){
			holder.bind(displayItems.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return displayItems.size();
		}

		@Override
		public int getItemViewType(int position){
			return displayItems.get(position).getType().ordinal();
		}

		@Override
		public int getImageCountForItem(int position){
			return displayItems.get(position).getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return displayItems.get(position).getImageRequest(image);
		}
	}
}
