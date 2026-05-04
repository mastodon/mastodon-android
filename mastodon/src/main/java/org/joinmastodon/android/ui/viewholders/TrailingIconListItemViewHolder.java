package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;

public class TrailingIconListItemViewHolder extends ListItemViewHolder<ListItem<?>>{
	private final ImageView trailingIcon;

	public TrailingIconListItemViewHolder(Context context, ViewGroup parent){
		super(context, R.layout.item_generic_list_trailing_icon, parent);
		trailingIcon=findViewById(R.id.trailing_icon);
		icon.setVisibility(View.GONE);
	}

	@Override
	protected void bindIcon(ListItem<?> item){
		if(item.iconRes!=0){
			trailingIcon.setVisibility(View.VISIBLE);
			trailingIcon.setImageResource(item.iconRes);
		}else{
			trailingIcon.setVisibility(View.GONE);
		}
	}
}
