package org.joinmastodon.android.ui.adapters;

import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.AvatarPileListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.viewholders.AvatarPileListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.CheckboxOrRadioListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.OptionsListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SwitchListItemViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.views.UsableRecyclerView;

public class GenericListItemsAdapter<T> extends UsableRecyclerView.Adapter<ListItemViewHolder<?>> implements ImageLoaderRecyclerAdapter{
	private List<ListItem<T>> items;

	public GenericListItemsAdapter(List<ListItem<T>> items){
		super(null);
		this.items=items;
	}

	public GenericListItemsAdapter(ListImageLoaderWrapper imgLoader, List<ListItem<T>> items){
		super(imgLoader);
		this.items=items;
	}

	@NonNull
	@Override
	public ListItemViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		if(viewType==R.id.list_item_simple || viewType==R.id.list_item_simple_tinted)
			return new SimpleListItemViewHolder(parent.getContext(), parent);
		if(viewType==R.id.list_item_switch || viewType==R.id.list_item_switch_separated)
			return new SwitchListItemViewHolder(parent.getContext(), parent, viewType==R.id.list_item_switch_separated);
		if(viewType==R.id.list_item_checkbox)
			return new CheckboxOrRadioListItemViewHolder(parent.getContext(), parent, false);
		if(viewType==R.id.list_item_radio)
			return new CheckboxOrRadioListItemViewHolder(parent.getContext(), parent, true);
		if(viewType==R.id.list_item_options)
			return new OptionsListItemViewHolder(parent.getContext(), parent);
		if(viewType==R.id.list_item_avatar_pile)
			return new AvatarPileListItemViewHolder(parent.getContext(), parent);

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

	@Override
	public int getImageCountForItem(int position){
		ListItem<?> item=items.get(position);
		if(item instanceof AvatarPileListItem<?> avatarPileListItem)
			return avatarPileListItem.avatars.size();
		return 0;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int position, int image){
		ListItem<?> item=items.get(position);
		if(item instanceof AvatarPileListItem<?> avatarPileListItem)
			return avatarPileListItem.avatars.get(image);
		return null;
	}
}
