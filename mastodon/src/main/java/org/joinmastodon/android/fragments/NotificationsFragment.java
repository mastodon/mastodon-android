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
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		ReblogOrReplyLineStatusDisplayItem titleItem=new ReblogOrReplyLineStatusDisplayItem(n.id, this, switch(n.type){
			case FOLLOW -> getString(R.string.user_followed_you, n.account.displayName);
			case FOLLOW_REQUEST -> getString(R.string.user_sent_follow_request, n.account.displayName);
			case MENTION -> getString(R.string.user_mentioned_you, n.account.displayName);
			case REBLOG -> getString(R.string.user_boosted, n.account.displayName);
			case FAVORITE -> getString(R.string.user_favorited, n.account.displayName);
			case POLL -> getString(R.string.poll_ended);
			case STATUS -> getString(R.string.user_posted, n.account.displayName);
		}, n.account.emojis, R.drawable.ic_fluent_arrow_reply_20_filled);
		if(n.status!=null){
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts);
			items.add(0, titleItem);
			return items;
		}else{
			return Collections.singletonList(titleItem);
		}
	}

	@Override
	protected void addAccountToKnown(Notification s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
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

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}
}
