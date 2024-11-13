package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetDomainBlockPreview;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class BlockDomainConfirmationSheet extends AccountRestrictionConfirmationSheet{
	private APIRequest<?> currentRequest;

	public BlockDomainConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback, ConfirmCallback blockUserConfirmCallback, String accountID){
		super(context, user, confirmCallback);
		titleView.setText(R.string.block_domain_confirm_title);
		confirmBtn.setText(R.string.do_block_server);
		secondaryBtn.setText(context.getString(R.string.block_user_x_instead, user.getDisplayUsername()));
		icon.setImageResource(R.drawable.ic_domain_disabled_24px);
		subtitleView.setText(user.getDomain());
		TextView relationsRow=addRow(R.drawable.ic_person_remove_24px, "");
		addRow(R.drawable.ic_campaign_24px, R.string.users_cant_see_blocked);
		addRow(R.drawable.ic_visibility_off_24px, R.string.you_wont_see_server_posts);
		addRow(R.drawable.ic_reply_24px, R.string.server_cant_mention_or_follow_you);
		addRow(R.drawable.ic_history_24px, R.string.server_can_interact_with_older);

		secondaryBtn.setOnClickListener(v->{
			if(loading)
				return;
			loading=true;
			secondaryBtn.setProgressBarVisible(true);
			blockUserConfirmCallback.onConfirmed(this::dismiss, ()->{
				secondaryBtn.setProgressBarVisible(false);
				loading=false;
			});
		});

		relationsRow.setVisibility(View.GONE);
		relationsRow.setTypeface(Typeface.DEFAULT_BOLD);
		currentRequest=new GetDomainBlockPreview(user.getDomain())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(GetDomainBlockPreview.Response result){
						currentRequest=null;
						if(result.followersCount>0 || result.followingCount>0){
							UiUtils.beginLayoutTransition(container);
							relationsRow.setVisibility(View.VISIBLE);
							if(result.followersCount>0 && result.followingCount>0){
								relationsRow.setText(context.getString(R.string.server_x_followers_and_following_will_be_removed,
										context.getResources().getQuantityString(R.plurals.will_lose_x_followers, result.followersCount, result.followersCount),
										context.getResources().getQuantityString(R.plurals.will_lose_x_following, result.followingCount, result.followingCount)));
							}else if(result.followersCount>0){
								relationsRow.setText(context.getResources().getQuantityString(R.plurals.server_x_followers_will_be_removed, result.followersCount, result.followersCount));
							}else{
								relationsRow.setText(context.getString(R.string.server_x_following_will_be_removed,
										context.getResources().getQuantityString(R.plurals.will_lose_x_following, result.followingCount, result.followingCount)));
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
					}
				})
				.exec(accountID);
	}

	@Override
	public void dismiss(){
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		super.dismiss();
	}
}
