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
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;

public abstract class StatusListFragment extends BaseStatusListFragment<Status>{
	protected EventListener eventListener=new EventListener();

	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		return StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, false, true);
	}

	@Override
	protected void addAccountToKnown(Status s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
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
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(status));
		if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
			args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
		Nav.go(getActivity(), ThreadFragment.class, args);
	}

	protected void onStatusCreated(StatusCreatedEvent ev){}

	protected void onStatusUpdated(StatusUpdatedEvent ev){
		ArrayList<Status> statusesForDisplayItems=new ArrayList<>();
		for(int i=0;i<data.size();i++){
			Status s=data.get(i);
			if(s.reblog!=null && s.reblog.id.equals(ev.status.id)){
				s.reblog=ev.status;
				statusesForDisplayItems.add(s);
			}else if(s.id.equals(ev.status.id)){
				data.set(i, ev.status);
				statusesForDisplayItems.add(ev.status);
			}
		}
		for(int i=0;i<preloadedData.size();i++){
			Status s=preloadedData.get(i);
			if(s.reblog!=null && s.reblog.id.equals(ev.status.id)){
				s.reblog=ev.status;
			}else if(s.id.equals(ev.status.id)){
				preloadedData.set(i, ev.status);
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
			StatusListFragment.this.onStatusCreated(ev);
		}

		@Subscribe
		public void onStatusUpdated(StatusUpdatedEvent ev){
			StatusListFragment.this.onStatusUpdated(ev);
		}

		@Subscribe
		public void onPollUpdated(PollUpdatedEvent ev){
			if(!ev.accountID.equals(accountID))
				return;
			for(Status status:data){
				Status contentStatus=status.getContentStatus();
				if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
					updatePoll(status.id, status, ev.poll);
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
