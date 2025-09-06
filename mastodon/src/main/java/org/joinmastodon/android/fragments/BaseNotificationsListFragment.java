package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.displayitems.FollowRequestActionsDisplayItem;
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
	private EventListener eventListener=new EventListener();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(eventListener);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(eventListener);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(NotificationViewModel n){
		StatusDisplayItem titleItem;
		if(n.notification.type==NotificationType.MENTION){
			if(n.status!=null){
				boolean replyToSelf=AccountSessionManager.get(accountID).self.id.equals(n.status.inReplyToAccountId);
				int icon=replyToSelf ? R.drawable.ic_reply_wght700_20px : R.drawable.ic_alternate_email_wght700fill1_20px;
				if(n.status.visibility==StatusPrivacy.DIRECT){
					titleItem=new ReblogOrReplyLineStatusDisplayItem(n.getID(), this, getActivity(), getString(replyToSelf ? R.string.private_reply : R.string.private_mention), null, icon, accountID);
				}else{
					titleItem=new ReblogOrReplyLineStatusDisplayItem(n.getID(), this, getActivity(), getString(replyToSelf ? R.string.post_header_reply : R.string.post_header_mention), null, icon, accountID);
				}
			}else{
				titleItem=null;
			}
		}else if(n.notification.type==NotificationType.STATUS){
			if(n.status!=null)
				titleItem=new ReblogOrReplyLineStatusDisplayItem(n.getID(), this, getActivity(), getString(R.string.user_just_posted), n.status.account, R.drawable.ic_notifications_wght700fill1_20px, accountID);
			else
				titleItem=null;
		}else if(n.notification.type==NotificationType.QUOTE){
			if(n.status!=null)
				titleItem=new ReblogOrReplyLineStatusDisplayItem(n.getID(), this, getActivity(), getString(R.string.user_quoted_post), n.status.account, R.drawable.ic_format_quote_wght700fill1_20px, accountID);
			else
				titleItem=null;
		}else{
			if(n.notification.type==NotificationType.SEVERED_RELATIONSHIPS || n.notification.type==NotificationType.MODERATION_WARNING)
				titleItem=new NotificationWithButtonStatusDisplayItem(n.getID(), this, getActivity(), n, accountID);
			else
				titleItem=new NotificationHeaderStatusDisplayItem(n.getID(), this, getActivity(), n, accountID);
		}
		if(n.status!=null){
			if(titleItem!=null && n.notification.type!=NotificationType.STATUS && n.notification.type!=NotificationType.MENTION && n.notification.type!=NotificationType.QUOTE){
				InlineStatusStatusDisplayItem inlineItem=new InlineStatusStatusDisplayItem(n.getID(), this, getActivity(), n.status, accountID);
				inlineItem.removeTopPadding=true;
				return List.of(titleItem, inlineItem);
			}else{
				ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, getActivity(), n.status, accountID, n, knownAccounts, titleItem!=null ? StatusDisplayItem.FLAG_NO_IN_REPLY_TO : 0);
				if(titleItem!=null)
					items.add(0, titleItem);
				return items;
			}
		}else if(titleItem!=null){
			if(n.notification.type==NotificationType.FOLLOW_REQUEST)
				return List.of(titleItem, new FollowRequestActionsDisplayItem(n.getID(), this, getActivity(), n));
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
			navigateToStatus(status);
		}else{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(n.accounts.get(0)));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}

	@Override
	public void onItemClick(String id, boolean quote){
		NotificationViewModel n=getNotificationByID(id);
		if(n.status!=null){
			Status status=n.status;
			navigateToStatus(quote ? status.quote.quotedStatus : status);
		}else{
			super.onItemClick(id, quote);
		}
	}

	@Override
	protected Status asStatus(NotificationViewModel s){
		return s.status;
	}

	protected NotificationViewModel getNotificationByID(String id){
		for(NotificationViewModel n:data){
			if(n.getID().equals(id))
				return n;
		}
		return null;
	}

	public void removeNotification(NotificationViewModel n){
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

	public class EventListener{
		@Subscribe
		public void onStatusUpdated(StatusUpdatedEvent ev){
			Status status=ev.status;

			ArrayList<NotificationViewModel> statusesForDisplayItems=new ArrayList<>();
			for(int i=0;i<data.size();i++){
				NotificationViewModel nvm=data.get(i);
				if(nvm.status==null)
					continue;
				Status s=nvm.status;
				if(s.id.equals(status.id)){
					nvm.status=status;
					statusesForDisplayItems.add(nvm);
				}
			}
			for(int i=0;i<preloadedData.size();i++){
				NotificationViewModel nvm=preloadedData.get(i);
				if(nvm.status!=null && nvm.status.id.equals(status.id)){
					nvm.status=status;
				}
			}

			if(statusesForDisplayItems.isEmpty())
				return;

			for(NotificationViewModel s:statusesForDisplayItems){
				int i=0;
				for(StatusDisplayItem item:displayItems){
					if(item.parentID.equals(s.getID())){
						int start=i;
						for(;i<displayItems.size();i++){
							if(!displayItems.get(i).parentID.equals(s.getID()))
								break;
						}
						List<StatusDisplayItem> postItems=displayItems.subList(start, i);
						postItems.clear();
						postItems.addAll(buildDisplayItems(s));
						int oldSize=i-start, newSize=postItems.size();
						if(oldSize==newSize){
							adapter.notifyItemRangeChanged(start, newSize);
						}else if(oldSize<newSize){
							adapter.notifyItemRangeChanged(start, oldSize);
							adapter.notifyItemRangeInserted(start+oldSize, newSize-oldSize);
						}else{
							adapter.notifyItemRangeChanged(start, newSize);
							adapter.notifyItemRangeRemoved(start+newSize, oldSize-newSize);
						}
						break;
					}
					i++;
				}
			}
		}
	}
}
