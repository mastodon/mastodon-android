package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.ListItemWithTrailingIconButton;
import org.joinmastodon.android.ui.utils.UiUtils;

public class TrailingIconButtonListItemViewHolder extends ListItemViewHolder<ListItemWithTrailingIconButton<?>>{
	private final ImageButton trailingIcon;

	public TrailingIconButtonListItemViewHolder(Context context, ViewGroup parent){
		super(context, R.layout.item_generic_list_trailing_icon_button, parent);
		trailingIcon=findViewById(R.id.trailing_icon);
		icon.setVisibility(View.GONE);
		trailingIcon.setOnClickListener(v->item.invokeOnClickListener());
	}

	@Override
	protected void bindIcon(ListItemWithTrailingIconButton<?> item){
		if(item.iconRes!=0){
			trailingIcon.setVisibility(View.VISIBLE);
			trailingIcon.setImageResource(item.iconRes);
			trailingIcon.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(itemView.getContext(), item.iconColorAttr==0 ? R.attr.colorM3OnSurfaceVariant : item.iconColorAttr)));
			trailingIcon.setContentDescription(item.buttonContentDescription);
		}else{
			trailingIcon.setVisibility(View.GONE);
		}
	}
}
