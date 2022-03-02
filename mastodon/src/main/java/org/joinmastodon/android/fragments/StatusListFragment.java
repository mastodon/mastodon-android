package org.joinmastodon.android.fragments;

import android.os.Bundle;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.parceler.Parcels;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;

public abstract class StatusListFragment extends BaseStatusListFragment<Status>{
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		return StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts);
	}

	@Override
	protected void addAccountToKnown(Status s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
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

	@Override
	protected void updatePoll(String itemID, Poll poll){
		Status status=getContentStatusByID(itemID);
		if(status==null)
			return;
		status.poll=poll;
		super.updatePoll(itemID, poll);
	}

	@Subscribe
	public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
		for(Status s:data){
			if(s.getContentStatus().id.equals(ev.id)){
				s.update(ev);
				for(int i=0;i<list.getChildCount();i++){
					RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
					if(holder instanceof FooterStatusDisplayItem.Holder && ((FooterStatusDisplayItem.Holder) holder).getItem().status==s.getContentStatus()){
						((FooterStatusDisplayItem.Holder) holder).rebind();
						return;
					}
				}
				return;
			}
		}
		for(Status s:preloadedData){
			if(s.id.equals(ev.id)){
				s.update(ev);
				return;
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
		data.remove(status);
		preloadedData.remove(status);
		HeaderStatusDisplayItem item=findItemOfType(ev.id, HeaderStatusDisplayItem.class);
		if(item==null)
			return;
		int index=displayItems.indexOf(item);
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(ev.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
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
}
