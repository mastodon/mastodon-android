package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.displayitems.InlineStatusStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.NotificationHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.NotificationWithButtonStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;

public abstract class BaseNotificationsListFragment extends BaseStatusListFragment<NotificationViewModel>{
	protected String maxID;
	protected View endMark;

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(NotificationViewModel n){
		StatusDisplayItem titleItem;
		if(n.notification.type==NotificationType.MENTION){
			titleItem=null;
		}else if(n.notification.type==NotificationType.STATUS){
			if(n.status!=null)
				titleItem=new ReblogOrReplyLineStatusDisplayItem(n.getID(), this, getString(R.string.user_just_posted, n.status.account.displayName), n.status.account.emojis, R.drawable.ic_notifications_wght700fill1_20px);
			else
				titleItem=null;
		}else{
			if(n.notification.type==NotificationType.SEVERED_RELATIONSHIPS || n.notification.type==NotificationType.MODERATION_WARNING)
				titleItem=new NotificationWithButtonStatusDisplayItem(n.getID(), this, n, accountID);
			else
				titleItem=new NotificationHeaderStatusDisplayItem(n.getID(), this, n, accountID);
		}
		if(n.status!=null){
			if(titleItem!=null && n.notification.type!=NotificationType.STATUS){
				InlineStatusStatusDisplayItem inlineItem=new InlineStatusStatusDisplayItem(n.getID(), this, n.status);
				inlineItem.removeTopPadding=true;
				return List.of(titleItem, inlineItem);
			}else{
				ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts, 0);
				if(titleItem!=null)
					items.add(0, titleItem);
				return items;
			}
		}else if(titleItem!=null){
			return List.of(titleItem);
		}else{
			return List.of();
		}
	}

	@Override
	protected void addAccountToKnown(NotificationViewModel s){
		for(Account a:s.accounts){
			if(!knownAccounts.containsKey(a.id))
				knownAccounts.put(a.id, a);
		}
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
	}

	@Override
	public void onItemClick(String id){
		NotificationViewModel n=getNotificationByID(id);
		if(n.status!=null){
			Status status=n.status;
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("status", Parcels.wrap(status.clone()));
			if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
				args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
			Nav.go(getActivity(), ThreadFragment.class, args);
		}else{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(n.accounts.get(0)));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}

	protected NotificationViewModel getNotificationByID(String id){
		for(NotificationViewModel n:data){
			if(n.getID().equals(id))
				return n;
		}
		return null;
	}

	protected void removeNotification(NotificationViewModel n){
		data.remove(n);
		preloadedData.remove(n);
		int index=-1;
		for(int i=0; i<displayItems.size(); i++){
			if(n.getID().equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index; lastIndex<displayItems.size(); lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(n.getID()))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	@Override
	protected View onCreateFooterView(LayoutInflater inflater){
		View v=inflater.inflate(R.layout.load_more_with_end_mark, null);
		endMark=v.findViewById(R.id.end_mark);
		endMark.setVisibility(View.GONE);
		return v;
	}
}
