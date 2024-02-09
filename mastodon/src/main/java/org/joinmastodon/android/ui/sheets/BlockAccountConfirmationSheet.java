package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;

import androidx.annotation.NonNull;

public class BlockAccountConfirmationSheet extends AccountRestrictionConfirmationSheet{
	public BlockAccountConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback){
		super(context, user, confirmCallback);
		titleView.setText(R.string.block_user_confirm_title);
		confirmBtn.setText(R.string.do_block);
		secondaryBtn.setVisibility(View.GONE);
		icon.setImageResource(R.drawable.ic_block_24px);
		subtitleView.setText(user.getDisplayUsername());
		addRow(R.drawable.ic_campaign_24px, R.string.user_can_see_blocked);
		addRow(R.drawable.ic_visibility_off_24px, R.string.user_cant_see_each_other_posts);
		addRow(R.drawable.ic_alternate_email_24px, R.string.you_wont_see_user_mentions);
		addRow(R.drawable.ic_reply_24px, R.string.user_cant_mention_or_follow_you);
	}
}
