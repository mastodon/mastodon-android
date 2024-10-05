package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsPolicy;
import org.joinmastodon.android.api.requests.notifications.SetNotificationsPolicy;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.NotificationsPolicy;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewcontrollers.GenericListItemsViewController;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.utils.ObjectIdComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class NotificationsListFragment extends BaseNotificationsListFragment{
	private boolean onlyMentions;
	private View tabBar;
	private View mentionsTab, allTab;
	private String unreadMarker, realUnreadMarker;
	private MenuItem markAllReadItem;
	private boolean reloadingFromCache;
	private ListItem<Void> requestsItem=new ListItem<>(R.string.filtered_notifications, 0, R.drawable.ic_inventory_2_24px, i->openNotificationRequests());
	private ArrayList<ListItem<Void>> requestsItems=new ArrayList<>();
	private GenericListItemsAdapter<Void> requestsRowAdapter=new GenericListItemsAdapter<>(requestsItems);
	private NotificationsPolicy lastPolicy;
	private boolean refreshAfterLoading;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setLayout(R.layout.fragment_notifications);
		E.register(this);
		onlyMentions=AccountSessionManager.get(accountID).isNotificationsMentionsOnly();
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
	protected void doLoadData(int offset, int count){
		if(!refreshing && !reloadingFromCache)
			endMark.setVisibility(View.GONE);
		if(offset==0)
			reloadPolicy();
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, refreshing && !reloadingFromCache, new SimpleCallback<>(this){
					@Override
					public void onSuccess(PaginatedResponse<List<NotificationViewModel>> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.items, !result.items.isEmpty());
						maxID=result.maxID;
						endMark.setVisibility(result.items.isEmpty() ? View.VISIBLE : View.GONE);
						reloadingFromCache=false;
						if(refreshAfterLoading){
							refreshAfterLoading=false;
							refresh();
						}
					}
				});
	}

	@Override
	protected void onShown(){
		super.onShown();
		unreadMarker=realUnreadMarker=AccountSessionManager.get(accountID).getLastKnownNotificationsMarker();
		if(canRefreshWithoutUpsettingUser()){
			if(dataLoading)
				refreshAfterLoading=true;
			else
				refresh();
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		resetUnreadBackground();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		tabBar=view.findViewById(R.id.tabbar);
		super.onViewCreated(view, savedInstanceState);

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
						String itemID=getNotificationByID(holder.getItemID()).notification.pageMaxId;
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

	@Subscribe
	public void onPollUpdated(PollUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(NotificationViewModel ntf:data){
			if(ntf.status==null)
				continue;
			Status contentStatus=ntf.status.getContentStatus();
			if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
				updatePoll(ntf.getID(), ntf.status, ev.poll);
			}
		}
	}

	@Subscribe
	public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		if(!ev.accountID.equals(accountID) || ev.isUnfollow)
			return;
		List<NotificationViewModel> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(n->n.status!=null && n.status.account.id.equals(ev.postsByAccountID))
				.collect(Collectors.toList());
		for(NotificationViewModel n:toRemove){
			removeNotification(n);
		}
	}

	@Override
	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return super.needDividerForExtraItem(child, bottomSibling, holder, siblingHolder) || (siblingHolder!=null && siblingHolder.getAbsoluteAdapterPosition()>=adapter.getItemCount()) || holder.getAbsoluteAdapterPosition()<requestsItems.size();
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
		refreshing=true;
		reloadingFromCache=true;
		loadData(0, 20);
		AccountSessionManager.get(accountID).setNotificationsMentionsOnly(onlyMentions);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.notifications, menu);
		markAllReadItem=menu.findItem(R.id.mark_all_read);
		MenuItem filters=menu.findItem(R.id.filters);
		filters.setVisible(lastPolicy!=null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.mark_all_read){
			markAsRead(true);
			resetUnreadBackground();
		}else if(id==R.id.filters){
			showFiltersAlert();
		}
		return true;
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(requestsRowAdapter);
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	private void markAsRead(boolean force){
		if(data.isEmpty())
			return;
		String id=data.get(0).notification.pageMaxId;
		if(force || ObjectIdComparator.INSTANCE.compare(id, realUnreadMarker)>0){
			new SaveMarkers(null, id).exec(accountID);
			AccountSessionManager.get(accountID).setNotificationsMarker(id, true);
			realUnreadMarker=id;
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

	@Override
	public void onAppendItems(List<NotificationViewModel> items){
		super.onAppendItems(items);
		// TODO
		if(data.isEmpty() || data.get(0).getID().equals(realUnreadMarker))
			return;
		for(NotificationViewModel n:items){
			if(ObjectIdComparator.INSTANCE.compare(n.notification.pageMinId, realUnreadMarker)<=0){
				markAsRead(false);
				break;
			}
		}
	}

	private boolean canRefreshWithoutUpsettingUser(){
		// TODO maybe reload notifications the same way we reload the home timelines, i.e. with gaps and stuff
		if(data.size()<=itemsPerPage)
			return true;
		for(int i=list.getChildCount()-1;i>=0;i--){
			if(list.getChildViewHolder(list.getChildAt(i)) instanceof StatusDisplayItem.Holder<?> itemHolder){
				String id=itemHolder.getItemID();
				for(int j=0;j<data.size();j++){
					if(data.get(j).getID().equals(id))
						return j<itemsPerPage; // Can refresh the list without losing scroll position if it is within the first page
				}
			}
		}
		return true;
	}

	private void updatePolicy(NotificationsPolicy policy){
		int count=policy.summary==null ? 0 : policy.summary.pendingRequestsCount;
		boolean isShown=!requestsItems.isEmpty();
		boolean needShow=count>0;
		if(isShown && !needShow){
			requestsItems.clear();
			requestsRowAdapter.notifyItemRemoved(0);
		}else if(!isShown && needShow){
			requestsItem.subtitle=getResources().getQuantityString(R.plurals.x_people_you_may_know, count, count);
			requestsItems.add(requestsItem);
			requestsRowAdapter.notifyItemInserted(0);
		}else if(isShown){
			requestsItem.subtitle=getResources().getQuantityString(R.plurals.x_people_you_may_know, count, count);
			requestsRowAdapter.notifyItemChanged(0);
		}
		lastPolicy=policy;
		invalidateOptionsMenu();
	}

	private void reloadPolicy(){
		new GetNotificationsPolicy()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(NotificationsPolicy policy){
						updatePolicy(policy);
					}

					@Override
					public void onError(ErrorResponse errorResponse){

					}
				})
				.exec(accountID);
	}

	private void showFiltersAlert(){
		GenericListItemsViewController<Void> controller=new GenericListItemsViewController<>(getActivity());
		Consumer<CheckableListItem<Void>> toggler=item->{
			item.toggle();
			controller.rebindItem(item);
		};
		CheckableListItem<Void> followingItem, followersItem, newAccountsItem, mentionsItem;
		List<ListItem<Void>> items=List.of(
				followingItem=new CheckableListItem<>(R.string.notification_filter_following, R.string.notification_filter_following_explanation, CheckableListItem.Style.CHECKBOX, lastPolicy.filterNotFollowing, toggler, true),
				followersItem=new CheckableListItem<>(R.string.notification_filter_followers, R.string.notification_filter_followers_explanation, CheckableListItem.Style.CHECKBOX, lastPolicy.filterNotFollowers, toggler, true),
				newAccountsItem=new CheckableListItem<>(R.string.notification_filter_new_accounts, R.string.notification_filter_new_accounts_explanation, CheckableListItem.Style.CHECKBOX, lastPolicy.filterNewAccounts, toggler, true),
				mentionsItem=new CheckableListItem<>(R.string.notification_filter_mentions, R.string.notification_filter_mentions_explanation, CheckableListItem.Style.CHECKBOX, lastPolicy.filterPrivateMentions, toggler, true)
		);
		controller.setItems(items);
		AlertDialog dlg=new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.filter_notifications)
				.setView(controller.getView())
				.setPositiveButton(R.string.save, null)
				.show();
		Button btn=dlg.getButton(Dialog.BUTTON_POSITIVE);
		btn.setOnClickListener(v->{
			UiUtils.showProgressForAlertButton(btn, true);
			NotificationsPolicy newPolicy=new NotificationsPolicy();
			newPolicy.filterNotFollowing=followingItem.checked;
			newPolicy.filterNotFollowers=followersItem.checked;
			newPolicy.filterNewAccounts=newAccountsItem.checked;
			newPolicy.filterPrivateMentions=mentionsItem.checked;
			new SetNotificationsPolicy(newPolicy)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(NotificationsPolicy policy){
							updatePolicy(policy);
							dlg.dismiss();
						}

						@Override
						public void onError(ErrorResponse errorResponse){
							Activity activity=getActivity();
							if(activity==null)
								return;
							UiUtils.showProgressForAlertButton(btn, false);
							errorResponse.showToast(activity);
						}
					})
					.exec(accountID);
		});
	}

	private void openNotificationRequests(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), NotificationRequestsFragment.class, args);
	}
}
