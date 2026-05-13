package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.fragments.collections.CollectionFragment;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.displayitems.CollectionStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FollowRequestActionsDisplayItem;
import org.joinmastodon.android.ui.displayitems.InlineStatusStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.NotificationHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.NotificationWithButtonStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.V;

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
			if(n.notification.type==NotificationType.FOLLOW_REQUEST){
				return List.of(titleItem, new FollowRequestActionsDisplayItem(n.getID(), this, getActivity(), n));
			}else if(n.notification.type==NotificationType.FALLBACK && !TextUtils.isEmpty(n.notification.fallback.summary)){
				SpannableStringBuilder parsedSummary=HtmlParser.parse(n.notification.fallback.summary, List.of(), List.of(), List.of(), accountID, n, getActivity());
				TextStatusDisplayItem textItem=new TextStatusDisplayItem(n.getID(), parsedSummary, this, getActivity(), null, accountID);
				return List.of(titleItem, textItem);
			}else if(n.notification.type==NotificationType.ADMIN_REPORT && !TextUtils.isEmpty(n.notification.report.comment)){
				return List.of(titleItem, new TextStatusDisplayItem(n.getID(), '"'+n.notification.report.comment+'"', this, getActivity(), null, accountID));
			}else if((n.notification.type==NotificationType.ADDED_TO_COLLECTION || n.notification.type==NotificationType.COLLECTION_UPDATE) && n.notification.collection!=null){
				CollectionStatusDisplayItem collectionItem=new CollectionStatusDisplayItem(n.getID(), this, getActivity(), n.notification.collection, knownAccounts);
				return List.of(titleItem, collectionItem);
			}
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
		}else if(n.notification.type==NotificationType.FALLBACK && !TextUtils.isEmpty(n.notification.fallback.description)){
			SpannableStringBuilder description=HtmlParser.parse(n.notification.fallback.description, List.of(), List.of(), List.of(), accountID, n, getActivity());
			LinkedTextView textView=new LinkedTextView(getActivity());
			textView.setTextAppearance(R.style.m3_body_large);
			textView.setPadding(V.dp(24), V.dp(24), V.dp(24), 0);
			textView.setText(description);
			AlertDialog alert=new M3AlertDialogBuilder(getActivity())
					.setView(textView)
					.setPositiveButton(R.string.ok, null)
					.show();
			for(LinkSpan link:description.getSpans(0, description.length(), LinkSpan.class)){
				link.preClickListener=alert::dismiss;
			}
		}else if(n.notification.type==NotificationType.ADMIN_REPORT){
			UiUtils.launchWebBrowser(getActivity(), "https://"+AccountSessionManager.get(accountID).domain+"/admin/reports/"+n.notification.report.id);
		}else if((n.notification.type==NotificationType.ADDED_TO_COLLECTION || n.notification.type==NotificationType.COLLECTION_UPDATE) && n.notification.collection!=null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putString("collection", n.notification.collection.id);
			args.putString("collectionTitle", n.notification.collection.name);
			args.putString("authorUsername", n.accounts.get(0).getUsername());
			Nav.go(getActivity(), CollectionFragment.class, args);
		}else if(!n.accounts.isEmpty()){
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

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(list.getChildViewHolder(view) instanceof TextStatusDisplayItem.Holder textHolder && textHolder.getItem().status==null){
					// Add extra padding after the summary that's part of some fallback notifications
					outRect.bottom+=V.dp(8);
				}
			}
		});
	}

	@Override
	public boolean isItemEnabled(StatusDisplayItem item){
		NotificationViewModel n=getNotificationByID(item.parentID);
		if(n.notification.type==NotificationType.FALLBACK && TextUtils.isEmpty(n.notification.fallback.description))
			return false;
		return super.isItemEnabled(item);
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
