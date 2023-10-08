package org.joinmastodon.android.ui.viewcontrollers;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ManageListsFragment;
import org.joinmastodon.android.model.FollowList;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;

public class HomeTimelineListsMenuController extends DropdownSubmenuController{
	private final List<FollowList> lists;
	private final HomeTimelineMenuController.Callback callback;

	public HomeTimelineListsMenuController(ToolbarDropdownMenuController dropdownController, HomeTimelineMenuController.Callback callback){
		super(dropdownController);
		this.lists=new ArrayList<>(callback.getLists());
		this.callback=callback;
		items=new ArrayList<>();
		for(FollowList l:lists){
			items.add(new Item<>(l.title, false, false, l, this::onListSelected));
		}
		items.add(new Item<Void>(dropdownController.getActivity().getString(R.string.manage_lists), false, true, i->{
			dropdownController.dismiss();
			Bundle args=new Bundle();
			args.putString("account", dropdownController.getAccountID());
			Nav.go(dropdownController.getActivity(), ManageListsFragment.class, args);
		}));
	}

	@Override
	protected CharSequence getBackItemTitle(){
		return dropdownController.getActivity().getString(R.string.lists);
	}

	private void onListSelected(Item<FollowList> item){
		callback.onListSelected(item.parentObject);
		dropdownController.dismiss();
	}
}
