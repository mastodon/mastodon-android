package org.joinmastodon.android.ui.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountBlocked;
import org.joinmastodon.android.api.requests.accounts.SetAccountMuted;
import org.joinmastodon.android.api.requests.statuses.DeleteStatus;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class UiUtils{
	private static Handler mainHandler=new Handler(Looper.getMainLooper());

	private UiUtils(){}

	public static void launchWebBrowser(Context context, String url){
		// TODO setting for custom tabs
		new CustomTabsIntent.Builder()
				.setShowTitle(true)
				.build()
				.launchUrl(context, Uri.parse(url));
	}

	public static String formatRelativeTimestamp(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<60_000L){
			return context.getString(R.string.time_seconds, diff/1000L);
		}else if(diff<3600_000L){
			return context.getString(R.string.time_minutes, diff/60_000L);
		}else if(diff<3600_000L*24L){
			return context.getString(R.string.time_hours, diff/3600_000L);
		}else{
			return context.getString(R.string.time_days, diff/(3600_000L*24L));
		}
	}

	public static String formatTimeLeft(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=t-now;
		if(diff<60_000L){
			int secs=(int)(diff/1000L);
			return context.getResources().getQuantityString(R.plurals.x_seconds_left, secs, secs);
		}else if(diff<3600_000L){
			int mins=(int)(diff/60_000L);
			return context.getResources().getQuantityString(R.plurals.x_minutes_left, mins, mins);
		}else if(diff<3600_000L*24L){
			int hours=(int)(diff/3600_000L);
			return context.getResources().getQuantityString(R.plurals.x_hours_left, hours, hours);
		}else{
			int days=(int)(diff/(3600_000L*24L));
			return context.getResources().getQuantityString(R.plurals.x_days_left, days, days);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(int n){
		if(n<1000)
			return String.format("%,d", n);
		else if(n<1_000_000)
			return String.format("%,.1fK", n/1000f);
		else
			return String.format("%,.1fM", n/1_000_000f);
	}

	/**
	 * Android 6.0 has a bug where start and end compound drawables don't get tinted.
	 * This works around it by setting the tint colors directly to the drawables.
	 * @param textView
	 */
	public static void fixCompoundDrawableTintOnAndroid6(TextView textView){
		Drawable[] drawables=textView.getCompoundDrawablesRelative();
		for(int i=0;i<drawables.length;i++){
			if(drawables[i]!=null){
				Drawable tinted=drawables[i].mutate();
				tinted.setTintList(textView.getTextColors());
				drawables[i]=tinted;
			}
		}
		textView.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
	}

	public static void runOnUiThread(Runnable runnable){
		mainHandler.post(runnable);
	}

	/** Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}. */
	public static int lerp(int startValue, int endValue, float fraction) {
		return startValue + Math.round(fraction * (endValue - startValue));
	}

	public static String getFileName(Uri uri){
		try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)){
			cursor.moveToFirst();
			String name=cursor.getString(0);
			if(name!=null)
				return name;
		}
		return uri.getLastPathSegment();
	}

	public static void loadCustomEmojiInTextView(TextView view){
		CharSequence _text=view.getText();
		if(!(_text instanceof Spanned))
			return;
		Spanned text=(Spanned)_text;
		CustomEmojiSpan[] spans=text.getSpans(0, text.length(), CustomEmojiSpan.class);
		if(spans.length==0)
			return;
		int emojiSize=V.dp(20);
		Map<Emoji, List<CustomEmojiSpan>> spansByEmoji=Arrays.stream(spans).collect(Collectors.groupingBy(s->s.emoji));
		for(Map.Entry<Emoji, List<CustomEmojiSpan>> emoji:spansByEmoji.entrySet()){
			ViewImageLoader.load(new ViewImageLoader.Target(){
				@Override
				public void setImageDrawable(Drawable d){
					if(d==null)
						return;
					for(CustomEmojiSpan span:emoji.getValue()){
						span.setDrawable(d);
					}
					view.invalidate();
				}

				@Override
				public View getView(){
					return view;
				}
			}, null, new UrlImageLoaderRequest(emoji.getKey().url, emojiSize, emojiSize), null, false, true);
		}
	}

	public static int getThemeColor(Context context, @AttrRes int attr){
		TypedArray ta=context.obtainStyledAttributes(new int[]{attr});
		int color=ta.getColor(0, 0xff00ff00);
		ta.recycle();
		return color;
	}

	public static void openProfileByID(Context context, String selfID, String id){
		Bundle args=new Bundle();
		args.putString("account", selfID);
		args.putString("profileAccountID", id);
		Nav.go((Activity)context, ProfileFragment.class, args);
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, Runnable onConfirmed){
		showConfirmationAlert(context, context.getString(title), context.getString(message), context.getString(confirmButton), onConfirmed);
	}

	public static void showConfirmationAlert(Context context, CharSequence title, CharSequence message, CharSequence confirmButton, Runnable onConfirmed){
		new M3AlertDialogBuilder(context)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(confirmButton, (dlg, i)->onConfirmed.run())
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	public static void confirmToggleBlockUser(Activity activity, String accountID, Account account, boolean currentlyBlocked, Consumer<Relationship> resultCallback){
		showConfirmationAlert(activity, activity.getString(currentlyBlocked ? R.string.confirm_unblock_title : R.string.confirm_block_title),
				activity.getString(currentlyBlocked ? R.string.confirm_unblock : R.string.confirm_block, account.displayName),
				activity.getString(currentlyBlocked ? R.string.do_block : R.string.do_unblock), ()->{
					new SetAccountBlocked(account.id, !currentlyBlocked)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Relationship result){
									resultCallback.accept(result);
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, false)
							.exec(accountID);
				});
	}

	public static void confirmToggleMuteUser(Activity activity, String accountID, Account account, boolean currentlyMuted, Consumer<Relationship> resultCallback){
		showConfirmationAlert(activity, activity.getString(currentlyMuted ? R.string.confirm_unmute_title : R.string.confirm_mute_title),
				activity.getString(currentlyMuted ? R.string.confirm_unmute : R.string.confirm_mute, account.displayName),
				activity.getString(currentlyMuted ? R.string.do_unmute : R.string.do_mute), ()->{
					new SetAccountMuted(account.id, !currentlyMuted)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Relationship result){
									resultCallback.accept(result);
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, false)
							.exec(accountID);
				});
	}

	public static void confirmDeletePost(Activity activity, String accountID, Status status, Consumer<Status> resultCallback){
		showConfirmationAlert(activity, R.string.confirm_delete_title, R.string.confirm_delete, R.string.delete, ()->{
			new DeleteStatus(status.id)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Status result){
							resultCallback.accept(result);
							E.post(new StatusDeletedEvent(status.id, accountID));
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.deleting, false)
					.exec(accountID);
		});
	}
}
