package org.joinmastodon.android.fragments;

import android.os.Bundle;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.CollectionStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;

public abstract class StatusListFragment extends BaseStatusListFragment<Status>{
	protected EventListener eventListener=new EventListener();

	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		return StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, true);
	}

	@Override
	protected void addAccountToKnown(Status s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.reblog!=null && !knownAccounts.containsKey(s.reblog.account.id))
			knownAccounts.put(s.reblog.account.id, s.reblog.account);
		if(s.quote!=null && s.quote.quotedStatus!=null && !knownAccounts.containsKey(s.quote.quotedStatus.account.id))
			knownAccounts.put(s.quote.quotedStatus.account.id, s.quote.quotedStatus.account);
	}

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
	public void onItemClick(String id){
		Status status=getContentStatusByID(id);
		if(status==null)
			return;
		navigateToStatus(status);
	}

	@Override
	public void onItemClick(String id, boolean quote){
		Status status=getContentStatusByID(id);
		if(status==null)
			return;
		navigateToStatus(quote ? status.quote.quotedStatus : status);
	}

	@Override
	protected Status asStatus(Status s){
		return s;
	}

	@Override
	protected Set<String> extractExtraAccountIDs(List<Status> items){
		HashSet<String> ids=new HashSet<>();
		for(Status s:items){
			extractCollectionAccountsFromStatus(ids, s.getContentStatus());
		}
		return ids;
	}

	@Override
	protected void updateItemsThatNeededExtraAccounts(){
		for(StatusDisplayItem item:displayItems){
			if(item instanceof CollectionStatusDisplayItem csdi){
				if(csdi.updateAccounts(knownAccounts)){
					StatusDisplayItem.Holder<CollectionStatusDisplayItem> holder=findHolderForItem(csdi);
					if(holder!=null){
						holder.rebind();
					}
				}
			}
		}
		if(imgLoader!=null)
			imgLoader.updateImages();
	}

	protected void onStatusCreated(Status status){}

	protected void onStatusUpdated(Status status){
		ArrayList<Status> statusesForDisplayItems=new ArrayList<>();
		for(int i=0;i<data.size();i++){
			Status s=data.get(i);
			if(s.reblog!=null && s.reblog.id.equals(status.id)){
				s.reblog=status.clone();
				statusesForDisplayItems.add(s);
			}else if(s.id.equals(status.id)){
				data.set(i, status);
				statusesForDisplayItems.add(status);
			}
		}
		for(int i=0;i<preloadedData.size();i++){
			Status s=preloadedData.get(i);
			if(s.reblog!=null && s.reblog.id.equals(status.id)){
				s.reblog=status.clone();
			}else if(s.id.equals(status.id)){
				preloadedData.set(i, status);
			}
		}

		if(statusesForDisplayItems.isEmpty())
			return;

		for(Status s:statusesForDisplayItems){
			int i=0;
			for(StatusDisplayItem item:displayItems){
				if(item.parentID.equals(s.id)){
					int start=i;
					for(;i<displayItems.size();i++){
						if(!displayItems.get(i).parentID.equals(s.id))
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

	protected Status getContentStatusByID(String id){
		Status s=getStatusByID(id);
		return s==null ? null : s.getContentStatus();
	}

	protected Status getStatusByID(String id){
		for(Status s:data){
			if(s.id.equals(id)){
				return s;
			}
		}
		for(Status s:preloadedData){
			if(s.id.equals(id)){
				return s;
			}
		}
		return null;
	}

	protected boolean shouldRemoveAccountPostsWhenUnfollowing(){
		return false;
	}

	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		List<Status> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(s->s.account.id.equals(ev.postsByAccountID) || (!ev.isUnfollow && s.reblog!=null && s.reblog.account.id.equals(ev.postsByAccountID)))
				.collect(Collectors.toList());
		for(Status s:toRemove){
			removeStatus(s);
		}
	}

	protected void removeStatus(Status status){
		data.remove(status);
		preloadedData.remove(status);
		int index=-1;
		for(int i=0;i<displayItems.size();i++){
			if(status.id.equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(status.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	public class EventListener{

		@Subscribe
		public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
			for(Status s:data){
				if(s.getContentStatus().id.equals(ev.id)){
					s.update(ev);
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder instanceof FooterStatusDisplayItem.Holder footer && footer.getItem().status==s.getContentStatus()){
							footer.rebind();
						}else if(holder instanceof ExtendedFooterStatusDisplayItem.Holder footer && footer.getItem().status==s.getContentStatus()){
							footer.rebind();
						}
					}
				}
			}
			for(Status s:preloadedData){
				if(s.id.equals(ev.id)){
					s.update(ev);
				}
			}
		}

		@Subscribe
		public void onStatusDeleted(StatusDeletedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			Status status=getStatusByID(ev.id);
			if(status==null)
				return;
			removeStatus(status);
		}

		@Subscribe
		public void onStatusCreated(StatusCreatedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			StatusListFragment.this.onStatusCreated(ev.status.clone());
		}

		@Subscribe
		public void onStatusUpdated(StatusUpdatedEvent ev){
			StatusListFragment.this.onStatusUpdated(ev.status);
		}

		@Subscribe
		public void onPollUpdated(PollUpdatedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			for(Status status:data){
				Status contentStatus=status.getContentStatus();
				if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
					updatePoll(status.id, contentStatus, ev.poll);
				}
			}
		}

		@Subscribe
		public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			if(ev.isUnfollow && !shouldRemoveAccountPostsWhenUnfollowing())
				return;
			StatusListFragment.this.onRemoveAccountPostsEvent(ev);
		}
	}
}
