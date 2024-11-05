package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.SectionHeaderListItem;

public class SectionHeaderListItemViewHolder extends ListItemViewHolder<SectionHeaderListItem>{
	public SectionHeaderListItemViewHolder(Context context, ViewGroup parent){
		super(context, R.layout.item_generic_list_header, parent);
	}

	@Override
	public void onBind(SectionHeaderListItem item){
		if(TextUtils.isEmpty(item.title))
			title.setText(item.titleRes);
		else
			title.setText(item.title);
	}
}
