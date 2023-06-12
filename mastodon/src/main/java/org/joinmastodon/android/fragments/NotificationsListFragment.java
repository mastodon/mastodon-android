package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.displayitems.NotificationHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.utils.ObjectIdComparator;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class NotificationsListFragment extends BaseStatusListFragment<Notification>{
	private boolean onlyMentions=true;
	private String maxID;
	private View tabBar;
	private View mentionsTab, allTab;
	private View endMark;
	private String unreadMarker, realUnreadMarker;
	private MenuItem markAllReadItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setLayout(R.layout.fragment_notifications);
		E.register(this);
		if(savedInstanceState!=null){
			onlyMentions=savedInstanceState.getBoolean("onlyMentions", true);
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.notifications);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		NotificationHeaderStatusDisplayItem titleItem;
		if(n.type==Notification.Type.MENTION || n.type==Notification.Type.STATUS){
			titleItem=null;
		}else{
			titleItem=new NotificationHeaderStatusDisplayItem(n.id, this, n, accountID);
			if(n.status!=null){
				n.status.card=null;
				n.status.spoilerText=null;
			}
		}
		if(n.status!=null){
			int flags=titleItem==null ? 0 : (StatusDisplayItem.FLAG_NO_FOOTER | StatusDisplayItem.FLAG_INSET | StatusDisplayItem.FLAG_NO_HEADER);
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts, flags);
			if(titleItem!=null)
				items.add(0, titleItem);
			return items;
		}else if(titleItem!=null){
			return Collections.singletonList(titleItem);
		}else{
			return Collections.emptyList();
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
		endMark.setVisibility(View.GONE);
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(PaginatedResponse<List<Notification>> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.items.stream().filter(n->n.type!=null).collect(Collectors.toList()), !result.items.isEmpty());
						maxID=result.maxID;
						endMark.setVisibility(result.items.isEmpty() ? View.VISIBLE : View.GONE);
					}
				});
	}

	@Override
	protected void onShown(){
		super.onShown();
		unreadMarker=realUnreadMarker=AccountSessionManager.get(accountID).getLastKnownNotificationsMarker();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		resetUnreadBackground();
	}

	@Override
	public void onItemClick(String id){
		Notification n=getNotificationByID(id);
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
			args.putParcelable("profileAccount", Parcels.wrap(n.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		tabBar=view.findViewById(R.id.tabbar);
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new InsetStatusItemDecoration(this));

		View tabBarItself=view.findViewById(R.id.tabbar_inner);
		tabBarItself.setOutlineProvider(OutlineProviders.roundedRect(20));
		tabBarItself.setClipToOutline(true);

		mentionsTab=view.findViewById(R.id.mentions_tab);
		allTab=view.findViewById(R.id.all_tab);
		mentionsTab.setOnClickListener(this::onTabClick);
		allTab.setOnClickListener(this::onTabClick);
		mentionsTab.setSelected(onlyMentions);
		allTab.setSelected(!onlyMentions);

		NestedRecyclerScrollView scroller=view.findViewById(R.id.scroller);
		scroller.setScrollableChildSupplier(()->list);
		scroller.setTakePriorityOverChildViews(true);

		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private Paint paint=new Paint();
			private Rect tmpRect=new Rect();

			{
				paint.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3SurfaceVariant));
			}

			@Override
			public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(TextUtils.isEmpty(unreadMarker))
					return;
				for(int i=0;i<parent.getChildCount();i++){
					View child=parent.getChildAt(i);
					if(parent.getChildViewHolder(child) instanceof StatusDisplayItem.Holder<?> holder){
						String itemID=holder.getItemID();
						if(ObjectIdComparator.INSTANCE.compare(itemID, unreadMarker)>0){
							parent.getDecoratedBoundsWithMargins(child, tmpRect);
							c.drawRect(tmpRect, paint);
						}
					}
				}
			}
		}, 0);
	}

	@Override
	protected List<View> getViewsForElevationEffect(){
		ArrayList<View> views=new ArrayList<>(super.getViewsForElevationEffect());
		views.add(tabBar);
		return views;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putBoolean("onlyMentions", onlyMentions);
	}

	private Notification getNotificationByID(String id){
		for(Notification n:data){
			if(n.id.equals(id))
				return n;
		}
		return null;
	}

	@Subscribe
	public void onPollUpdated(PollUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(Notification ntf:data){
			if(ntf.status==null)
				continue;
			Status contentStatus=ntf.status.getContentStatus();
			if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
				updatePoll(ntf.id, ntf.status, ev.poll);
			}
		}
	}

	@Subscribe
	public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		if(!ev.accountID.equals(accountID) || ev.isUnfollow)
			return;
		List<Notification> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(n->n.account!=null && n.account.id.equals(ev.postsByAccountID))
				.collect(Collectors.toList());
		for(Notification n:toRemove){
			removeNotification(n);
		}
	}

	private void removeNotification(Notification n){
		data.remove(n);
		preloadedData.remove(n);
		int index=-1;
		for(int i=0;i<displayItems.size();i++){
			if(n.id.equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(n.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	private void onTabClick(View v){
		boolean newOnlyMentions=v.getId()==R.id.mentions_tab;
		if(newOnlyMentions==onlyMentions)
			return;
		onlyMentions=newOnlyMentions;
		mentionsTab.setSelected(onlyMentions);
		allTab.setSelected(!onlyMentions);
		maxID=null;
		showProgress();
		loadData(0, 20);
		refreshing=true;
	}

	@Override
	protected View onCreateFooterView(LayoutInflater inflater){
		View v=inflater.inflate(R.layout.load_more_with_end_mark, null);
		endMark=v.findViewById(R.id.end_mark);
		endMark.setVisibility(View.GONE);
		return v;
	}

	@Override
	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return super.needDividerForExtraItem(child, bottomSibling, holder, siblingHolder) || (siblingHolder!=null && siblingHolder.getAbsoluteAdapterPosition()>=adapter.getItemCount());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.notifications, menu);
		markAllReadItem=menu.findItem(R.id.mark_all_read);
		updateMarkAllReadButton();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==R.id.mark_all_read){
			markAsRead();
			resetUnreadBackground();
		}
		return true;
	}

	private void markAsRead(){
		String id=data.get(0).id;
		if(ObjectIdComparator.INSTANCE.compare(id, realUnreadMarker)>0){
			new SaveMarkers(null, id).exec(accountID);
			AccountSessionManager.get(accountID).setNotificationsMarker(id, true);
			realUnreadMarker=id;
			updateMarkAllReadButton();
		}
	}

	private void resetUnreadBackground(){
		unreadMarker=realUnreadMarker;
		list.invalidate();
	}

	@Override
	public void onRefresh(){
		super.onRefresh();
		resetUnreadBackground();
		AccountSessionManager.get(accountID).reloadNotificationsMarker(m->{
			unreadMarker=realUnreadMarker=m;
		});
	}

	private void updateMarkAllReadButton(){
		markAllReadItem.setEnabled(!data.isEmpty() && !realUnreadMarker.equals(data.get(0).id));
	}

	@Override
	public void onAppendItems(List<Notification> items){
		super.onAppendItems(items);
		if(data.isEmpty() || data.get(0).id.equals(realUnreadMarker))
			return;
		for(Notification n:items){
			if(ObjectIdComparator.INSTANCE.compare(n.id, realUnreadMarker)<=0){
				markAsRead();
				break;
			}
		}
	}
}
