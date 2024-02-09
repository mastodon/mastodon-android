package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;

import androidx.annotation.NonNull;

public class MuteAccountConfirmationSheet extends AccountRestrictionConfirmationSheet{
	public MuteAccountConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback){
		super(context, user, confirmCallback);
		titleView.setText(R.string.mute_user_confirm_title);
		confirmBtn.setText(R.string.do_mute);
		secondaryBtn.setVisibility(View.GONE);
		icon.setImageResource(R.drawable.ic_volume_off_24px);
		subtitleView.setText(user.getDisplayUsername());
		addRow(R.drawable.ic_campaign_24px, R.string.user_wont_know_muted);
		addRow(R.drawable.ic_visibility_off_24px, R.string.user_can_still_see_your_posts);
		addRow(R.drawable.ic_alternate_email_24px, R.string.you_wont_see_user_mentions);
		addRow(R.drawable.ic_reply_24px, R.string.user_can_mention_and_follow_you);
	}
}
