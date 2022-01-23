package org.joinmastodon.android.fragments;

import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.List;

public abstract class StatusListFragment extends BaseStatusListFragment<Status>{
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		return StatusDisplayItem.buildItems(this, s, accountID, s);
	}
}
