package org.joinmastodon.android.ui;

import android.content.Context;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import androidx.annotation.NonNull;

public class OldPostPreReplySheet extends PreReplySheet{
	public OldPostPreReplySheet(@NonNull Context context, ResultListener resultListener, Status status){
		super(context, resultListener);
		int months=(int)status.createdAt.atZone(ZoneId.systemDefault()).until(ZonedDateTime.now(), ChronoUnit.MONTHS);
		String monthsStr=months>24 ? context.getString(R.string.more_than_two_years) : context.getResources().getQuantityString(R.plurals.x_months, months, months);
		title.setText(context.getString(R.string.old_post_sheet_title, monthsStr));
		text.setText(R.string.old_post_sheet_text);
		icon.setImageResource(R.drawable.ic_history_24px);
	}
}
