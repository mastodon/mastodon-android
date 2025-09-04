package org.joinmastodon.android.fragments;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.RespondToNotificationRequest;
import org.joinmastodon.android.events.NotificationRequestRespondedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class AccountNotificationsListFragment extends BaseNotificationsListFragment{
	private Account account;
	private String requestID;
	private TextView expandedTitle;
	private boolean choiceMade, allowed;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		account=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		requestID=getArguments().getString("requestID");
		setTitleMarqueeEnabled(false);
		loadData();
		setTitle(getString(R.string.notifications_from_user, account.displayName));
		setHasOptionsMenu(true);
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(!refreshing && endMark!=null)
			endMark.setVisibility(View.GONE);
//		currentRequest=new GetNotificationsV2(offset==0 ? null : maxID, count, EnumSet.allOf(NotificationType.class), account.id)
//				.setCallback(new SimpleCallback<>(this){
//					@Override
//					public void onSuccess(List<NotificationViewModel> result){
//						onDataLoaded(result, !result.isEmpty());
//						maxID=result.isEmpty() ? null : result.get(result.size()-1).id;
//						endMark.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
//					}
//				})
//				.exec(accountID);
	}

	@Override
	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return super.needDividerForExtraItem(child, bottomSibling, holder, siblingHolder) || (siblingHolder!=null && siblingHolder.getAbsoluteAdapterPosition()>=list.getAdapter().getItemCount());
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();

		expandedTitle=(TextView) LayoutInflater.from(getActivity()).inflate(R.layout.expanded_title_medium, list, false);
		expandedTitle.setText(getTitle());
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(expandedTitle));

		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(recyclerView.getChildCount()==0)
					return;
				float fraction;
				View topChild=recyclerView.getChildAt(0);
				if(recyclerView.getChildAdapterPosition(topChild)>0){
					fraction=1;
				}else{
					fraction=(-topChild.getTop())/(float)(topChild.getHeight()-topChild.getPaddingBottom());
				}
				expandedTitle.setAlpha(1f-fraction);
				toolbarTitleView.setAlpha(fraction);
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.notification_request, menu);
		MenuItem mute=menu.findItem(R.id.mute);
		MenuItem allow=menu.findItem(R.id.allow);
		if(choiceMade && allowed){
			allow.setIcon(R.drawable.ic_check_wght700_24px);
			tintMenuIcon(allow, R.attr.colorM3Primary);
		}else{
			tintMenuIcon(allow, R.attr.colorM3OnSurfaceVariant);
		}
		if(choiceMade && !allowed){
			mute.setIcon(R.drawable.ic_delete_wght700_24px);
			tintMenuIcon(mute, R.attr.colorM3Primary);
		}else{
			tintMenuIcon(mute, R.attr.colorM3OnSurfaceVariant);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(choiceMade)
			return true;
		allowed=item.getItemId()==R.id.allow;
		new RespondToNotificationRequest(requestID, allowed)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Void result){
						choiceMade=true;
						invalidateOptionsMenu();
						E.post(new NotificationRequestRespondedEvent(accountID, requestID));
						new Snackbar.Builder(getActivity())
								.setText(getString(allowed ? R.string.notifications_allowed : R.string.notifications_muted, account.displayName))
								.show();
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
		return true;
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(NotificationViewModel n){
		if(n.notification.type==NotificationType.MENTION || n.notification.type==NotificationType.STATUS || n.notification.type==NotificationType.QUOTE){
			return StatusDisplayItem.buildItems(this, getActivity(), n.status, accountID, n, knownAccounts, StatusDisplayItem.FLAG_MEDIA_FORCE_HIDDEN);
		}
		return super.buildDisplayItems(n);
	}

	@Override
	protected boolean wantsToolbarMenuIconsTinted(){
		return false;
	}

	private void tintMenuIcon(MenuItem item, int color){
		int tintColor=UiUtils.getThemeColor(getActivity(), color);
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O){
			Drawable icon=item.getIcon();
			if(icon!=null && icon.getColorFilter()==null){
				icon=icon.mutate();
				icon.setTintList(ColorStateList.valueOf(tintColor));
				item.setIcon(icon);
			}
		}else{
			item.setIconTintList(ColorStateList.valueOf(tintColor));
		}
	}
}
