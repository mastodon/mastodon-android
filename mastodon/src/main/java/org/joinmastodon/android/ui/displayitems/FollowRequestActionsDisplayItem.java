package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.AcceptFollowRequest;
import org.joinmastodon.android.api.requests.accounts.RejectFollowRequest;
import org.joinmastodon.android.fragments.BaseNotificationsListFragment;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;

import me.grishka.appkit.api.SimpleCallback;

public class FollowRequestActionsDisplayItem extends StatusDisplayItem{
	public final NotificationViewModel notification;

	public FollowRequestActionsDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, NotificationViewModel notification){
		super(parentID, parentFragment);
		this.notification=notification;
	}

	@Override
	public Type getType(){
		return Type.FOLLOW_REQUEST_ACTIONS;
	}

	public static class Holder extends StatusDisplayItem.Holder<FollowRequestActionsDisplayItem>{
		private final Button approveButton, declineButton;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_follow_request_buttons, parent);
			approveButton=findViewById(R.id.notification_approve);
			declineButton=findViewById(R.id.notification_decline);

			approveButton.setOnClickListener(this::acceptFollowRequest);
			declineButton.setOnClickListener(this::rejectFollowRequest);
		}

		@Override
		public void onBind(FollowRequestActionsDisplayItem item){

		}

		private void acceptFollowRequest(View v){
			BaseStatusListFragment<?> parentFragment=this.getItem().parentFragment;

			new AcceptFollowRequest(this.getItem().notification.accounts.get(0).id)
					.setCallback(new SimpleCallback<>(this.getItem().parentFragment){
						@Override
						public void onSuccess(Relationship result){
							parentFragment.removeDisplayItem(item);
							parentFragment.refresh();
						}
					})
					.wrapProgress(parentFragment.getActivity(), R.string.loading, true)
					.exec(parentFragment.getAccountID());
		}

		private void rejectFollowRequest(View v){
			BaseStatusListFragment<?> parentFragment=this.getItem().parentFragment;

			new RejectFollowRequest(this.getItem().notification.accounts.get(0).id)
					.setCallback(new SimpleCallback<>(this.getItem().parentFragment){
						@Override
						public void onSuccess(Relationship result){
							BaseNotificationsListFragment fragment=(BaseNotificationsListFragment) parentFragment;
							fragment.removeNotification(item.notification);
						}
					})
					.wrapProgress(parentFragment.getActivity(), R.string.loading, true)
					.exec(parentFragment.getAccountID());
		}
	}
}
