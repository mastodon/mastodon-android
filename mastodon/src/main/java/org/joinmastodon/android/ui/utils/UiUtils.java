package org.joinmastodon.android.ui.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountBlocked;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetAccountMuted;
import org.joinmastodon.android.api.requests.accounts.SetDomainBlocked;
import org.joinmastodon.android.api.requests.statuses.DeleteStatus;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.parceler.Parcels;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import okhttp3.MediaType;

public class UiUtils{
	private static Handler mainHandler=new Handler(Looper.getMainLooper());
	private static final DateTimeFormatter DATE_FORMATTER_SHORT_WITH_YEAR=DateTimeFormatter.ofPattern("d MMM uuuu"), DATE_FORMATTER_SHORT=DateTimeFormatter.ofPattern("d MMM");
	public static final DateTimeFormatter DATE_TIME_FORMATTER=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);

	private UiUtils(){}

	public static void launchWebBrowser(Context context, String url){
		try{
			if(GlobalUserPreferences.useCustomTabs){
				new CustomTabsIntent.Builder()
						.setShowTitle(true)
						.build()
						.launchUrl(context, Uri.parse(url));
			}else{
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		}catch(ActivityNotFoundException x){
			Toast.makeText(context, R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	public static String formatRelativeTimestamp(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<1000L){
			return context.getString(R.string.time_now);
		}else if(diff<60_000L){
			return context.getString(R.string.time_seconds, diff/1000L);
		}else if(diff<3600_000L){
			return context.getString(R.string.time_minutes, diff/60_000L);
		}else if(diff<3600_000L*24L){
			return context.getString(R.string.time_hours, diff/3600_000L);
		}else{
			int days=(int)(diff/(3600_000L*24L));
			if(days>30){
				ZonedDateTime dt=instant.atZone(ZoneId.systemDefault());
				if(dt.getYear()==ZonedDateTime.now().getYear()){
					return DATE_FORMATTER_SHORT.format(dt);
				}else{
					return DATE_FORMATTER_SHORT_WITH_YEAR.format(dt);
				}
			}
			return context.getString(R.string.time_days, days);
		}
	}

	public static String formatRelativeTimestampAsMinutesAgo(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<1000L){
			return context.getString(R.string.time_just_now);
		}else if(diff<60_000L){
			int secs=(int)(diff/1000L);
			return context.getResources().getQuantityString(R.plurals.x_seconds_ago, secs, secs);
		}else if(diff<3600_000L){
			int mins=(int)(diff/60_000L);
			return context.getResources().getQuantityString(R.plurals.x_minutes_ago, mins, mins);
		}else{
			return DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
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
		if(n<1000){
			return String.format("%,d", n);
		}else if(n<1_000_000){
			float a=n/1000f;
			return a>99f ? String.format("%,dK", (int)Math.floor(a)) : String.format("%,.1fK", a);
		}else{
			float a=n/1_000_000f;
			return a>99f ? String.format("%,dM", (int)Math.floor(a)) : String.format("%,.1fM", n/1_000_000f);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(long n){
		if(n<1_000_000_000L)
			return abbreviateNumber((int)n);

		double a=n/1_000_000_000.0;
		return a>99f ? String.format("%,dB", (int)Math.floor(a)) : String.format("%,.1fB", n/1_000_000_000.0);
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

	public static void runOnUiThread(Runnable runnable, long delay){
		mainHandler.postDelayed(runnable, delay);
	}

	public static void removeCallbacks(Runnable runnable){
		mainHandler.removeCallbacks(runnable);
	}

	/** Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}. */
	public static int lerp(int startValue, int endValue, float fraction) {
		return startValue + Math.round(fraction * (endValue - startValue));
	}

	public static String getFileName(Uri uri){
		if(uri.getScheme().equals("content")){
			try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)){
				cursor.moveToFirst();
				String name=cursor.getString(0);
				if(name!=null)
					return name;
			}catch(Throwable ignore){}
		}
		return uri.getLastPathSegment();
	}

	public static String formatFileSize(Context context, long size, boolean atLeastKB){
		if(size<1024 && !atLeastKB){
			return context.getString(R.string.file_size_bytes, size);
		}else if(size<1024*1024){
			return context.getString(R.string.file_size_kb, size/1024.0);
		}else if(size<1024*1024*1024){
			return context.getString(R.string.file_size_mb, size/(1024.0*1024.0));
		}else{
			return context.getString(R.string.file_size_gb, size/(1024.0*1024.0*1024.0));
		}
	}

	public static MediaType getFileMediaType(File file){
		String name=file.getName();
		return MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(name.lastIndexOf('.')+1)));
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

	public static void openHashtagTimeline(Context context, String accountID, String hashtag){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("hashtag", hashtag);
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
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
				activity.getString(currentlyBlocked ? R.string.do_unblock : R.string.do_block), ()->{
					new SetAccountBlocked(account.id, !currentlyBlocked)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Relationship result){
									resultCallback.accept(result);
									if(!currentlyBlocked){
										E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
									}
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

	public static void confirmToggleBlockDomain(Activity activity, String accountID, String domain, boolean currentlyBlocked, Runnable resultCallback){
		showConfirmationAlert(activity, activity.getString(currentlyBlocked ? R.string.confirm_unblock_domain_title : R.string.confirm_block_domain_title),
				activity.getString(currentlyBlocked ? R.string.confirm_unblock : R.string.confirm_block, domain),
				activity.getString(currentlyBlocked ? R.string.do_unblock : R.string.do_block), ()->{
					new SetDomainBlocked(domain, !currentlyBlocked)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Object result){
									resultCallback.run();
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
									if(!currentlyMuted){
										E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
									}
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
							AccountSessionManager.getInstance().getAccount(accountID).getCacheController().deleteStatus(status.id);
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

	public static void setRelationshipToActionButton(Relationship relationship, Button button){
		boolean secondaryStyle;
		if(relationship.blocking){
			button.setText(R.string.button_blocked);
			secondaryStyle=true;
		}else if(relationship.blockedBy){
			button.setText(R.string.button_follow);
			secondaryStyle=false;
		}else if(relationship.requested){
			button.setText(R.string.button_follow_pending);
			secondaryStyle=true;
		}else if(!relationship.following){
			button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
			secondaryStyle=false;
		}else{
			button.setText(R.string.button_following);
			secondaryStyle=true;
		}

		button.setEnabled(!relationship.blockedBy);
		int attr=secondaryStyle ? R.attr.secondaryButtonStyle : android.R.attr.buttonStyle;
		TypedArray ta=button.getContext().obtainStyledAttributes(new int[]{attr});
		int styleRes=ta.getResourceId(0, 0);
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		if(relationship.blocking)
			button.setTextColor(button.getResources().getColorStateList(R.color.error_600));
		else
			button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	public static void performAccountAction(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback){
		if(relationship.blocking){
			confirmToggleBlockUser(activity, accountID, account, true, resultCallback);
		}else if(relationship.muting){
			confirmToggleMuteUser(activity, accountID, account, true, resultCallback);
		}else{
			progressCallback.accept(true);
			new SetAccountFollowed(account.id, !relationship.following && !relationship.requested, true)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							progressCallback.accept(false);
							if(!result.following){
								E.post(new RemoveAccountPostsEvent(accountID, account.id, true));
							}
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
							progressCallback.accept(false);
						}
					})
					.exec(accountID);
		}
	}

	public static <T> void updateList(List<T> oldList, List<T> newList, RecyclerView list, RecyclerView.Adapter<?> adapter, BiPredicate<T, T> areItemsSame){
		// Save topmost item position and offset because for some reason RecyclerView would scroll the list to weird places when you insert items at the top
		int topItem, topItemOffset;
		if(list.getChildCount()==0){
			topItem=topItemOffset=0;
		}else{
			View child=list.getChildAt(0);
			topItem=list.getChildAdapterPosition(child);
			topItemOffset=child.getTop();
		}
		DiffUtil.calculateDiff(new DiffUtil.Callback(){
			@Override
			public int getOldListSize(){
				return oldList.size();
			}

			@Override
			public int getNewListSize(){
				return newList.size();
			}

			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition){
				return areItemsSame.test(oldList.get(oldItemPosition), newList.get(newItemPosition));
			}

			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition){
				return true;
			}
		}).dispatchUpdatesTo(adapter);
		list.scrollToPosition(topItem);
		list.scrollBy(0, topItemOffset);
	}

	public static Bitmap getBitmapFromDrawable(Drawable d){
		if(d instanceof BitmapDrawable)
			return ((BitmapDrawable) d).getBitmap();
		Bitmap bitmap=Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		d.draw(new Canvas(bitmap));
		return bitmap;
	}

	public static void enablePopupMenuIcons(Context context, PopupMenu menu){
		Menu m=menu.getMenu();
		if(Build.VERSION.SDK_INT>=29){
			menu.setForceShowIcon(true);
		}else{
			try{
				Method setOptionalIconsVisible=m.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
				setOptionalIconsVisible.setAccessible(true);
				setOptionalIconsVisible.invoke(m, true);
			}catch(Exception ignore){}
		}
		ColorStateList iconTint=ColorStateList.valueOf(UiUtils.getThemeColor(context, android.R.attr.textColorSecondary));
		for(int i=0;i<m.size();i++){
			MenuItem item=m.getItem(i);
			Drawable icon=item.getIcon().mutate();
			if(Build.VERSION.SDK_INT>=26){
				item.setIconTintList(iconTint);
			}else{
				icon.setTintList(iconTint);
			}
			icon=new InsetDrawable(icon, V.dp(8), 0, 0, 0);
			item.setIcon(icon);
			SpannableStringBuilder ssb=new SpannableStringBuilder(item.getTitle());
			ssb.insert(0, " ");
			ssb.setSpan(new SpacerSpan(V.dp(24), 1), 0, 1, 0);
			ssb.append(" ", new SpacerSpan(V.dp(8), 1), 0);
			item.setTitle(ssb);
		}
	}

	public static void setUserPreferredTheme(Context context){
		context.setTheme(switch(GlobalUserPreferences.theme){
			case AUTO -> GlobalUserPreferences.trueBlackTheme ? R.style.Theme_Mastodon_AutoLightDark_TrueBlack : R.style.Theme_Mastodon_AutoLightDark;
			case LIGHT -> R.style.Theme_Mastodon_Light;
			case DARK -> GlobalUserPreferences.trueBlackTheme ? R.style.Theme_Mastodon_Dark_TrueBlack : R.style.Theme_Mastodon_Dark;
		});
	}

	public static boolean isDarkTheme(){
		if(GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO)
			return (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
		return GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK;
	}

	public static void openURL(Context context, String accountID, String url){
		Uri uri=Uri.parse(url);
		if(accountID!=null && "https".equals(uri.getScheme()) && AccountSessionManager.getInstance().getAccount(accountID).domain.equalsIgnoreCase(uri.getAuthority())){
			List<String> path=uri.getPathSegments();
			// Match URLs like https://mastodon.social/@Gargron/108132679274083591
			if(path.size()==2 && path.get(0).matches("^@[a-zA-Z0-9_]+$") && path.get(1).matches("^[0-9]+$")){
				new GetStatusByID(path.get(1))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Status result){
								Bundle args=new Bundle();
								args.putString("account", accountID);
								args.putParcelable("status", Parcels.wrap(result));
								Nav.go((Activity) context, ThreadFragment.class, args);
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(context);
								launchWebBrowser(context, url);
							}
						})
						.wrapProgress((Activity)context, R.string.loading, true)
						.exec(accountID);
				return;
			}
		}
		launchWebBrowser(context, url);
	}

	private static String getSystemProperty(String key){
		try{
			Class<?> props=Class.forName("android.os.SystemProperties");
			Method get=props.getMethod("get", String.class);
			return (String)get.invoke(null, key);
		}catch(Exception ignore){}
		return null;
	}

	public static boolean isMIUI(){
		return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"));
	}
}
