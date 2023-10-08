package org.joinmastodon.android.ui.viewcontrollers;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.FollowList;

import java.util.List;

public class HomeTimelineMenuController extends DropdownSubmenuController{
	private Callback callback;

	public HomeTimelineMenuController(ToolbarDropdownMenuController dropdownController, Callback callback){
		super(dropdownController);
		this.callback=callback;
		items=List.of(
				new Item<Void>(R.string.timeline_following, false, false, i->{
					callback.onFollowingSelected();
					dropdownController.dismiss();
				}),
				new Item<Void>(R.string.local_timeline, false, false, i->{
					callback.onLocalSelected();
					dropdownController.dismiss();
				}),
				new Item<Void>(R.string.lists, true, true, i->dropdownController.pushSubmenuController(new HomeTimelineListsMenuController(dropdownController, callback))),
				new Item<Void>(R.string.followed_hashtags, true, false, i->dropdownController.pushSubmenuController(new HomeTimelineHashtagsMenuController(dropdownController)))
		);
	}

	@Override
	protected CharSequence getBackItemTitle(){
		return null;
	}

	public interface Callback{
		void onFollowingSelected();
		void onLocalSelected();
		List<FollowList> getLists();
		void onListSelected(FollowList list);
	}
}
