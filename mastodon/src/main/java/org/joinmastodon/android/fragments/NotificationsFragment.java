package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotifications;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class NotificationsFragment extends BaseStatusListFragment<Notification>{
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.notifications);
		loadData();
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		ReblogOrReplyLineStatusDisplayItem titleItem=new ReblogOrReplyLineStatusDisplayItem(n.id, switch(n.type){
			case FOLLOW -> getString(R.string.user_followed_you, n.account.displayName);
			case FOLLOW_REQUEST -> getString(R.string.user_sent_follow_request, n.account.displayName);
			case MENTION -> getString(R.string.user_mentioned_you, n.account.displayName);
			case REBLOG -> getString(R.string.user_boosted, n.account.displayName);
			case FAVORITE -> getString(R.string.user_favorited, n.account.displayName);
			case POLL -> getString(R.string.poll_ended);
			case STATUS -> getString(R.string.user_posted, n.account.displayName);
		});
		if(n.status!=null){
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n);
			items.add(0, titleItem);
			return items;
		}else{
			return Collections.singletonList(titleItem);
		}
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetNotifications(offset>0 ? getMaxID() : null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Notification> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}
}
