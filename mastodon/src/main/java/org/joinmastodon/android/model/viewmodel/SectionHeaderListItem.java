package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import androidx.annotation.StringRes;

public class SectionHeaderListItem extends ListItem<Void>{
	public SectionHeaderListItem(String title){
		super(title, null, null);
	}

	public SectionHeaderListItem(@StringRes int title){
		super(title, 0, null);
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_section_header;
	}
}
