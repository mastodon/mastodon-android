package org.joinmastodon.android.ui.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.transition.ChangeBounds;
import android.transition.ChangeScroll;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.E;
import org.joinmastodon.android.FileProvider;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountBlocked;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetAccountMuted;
import org.joinmastodon.android.api.requests.accounts.SetDomainBlocked;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
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
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.ColorContrastMode;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.sheets.BlockAccountConfirmationSheet;
import org.joinmastodon.android.ui.sheets.BlockDomainConfirmationSheet;
import org.joinmastodon.android.ui.sheets.MuteAccountConfirmationSheet;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import okhttp3.MediaType;

public class UiUtils{
	private static Handler mainHandler=new Handler(Looper.getMainLooper());
	private static final DateTimeFormatter DATE_FORMATTER_SHORT_WITH_YEAR=DateTimeFormatter.ofPattern("d MMM uuuu"), DATE_FORMATTER_SHORT=DateTimeFormatter.ofPattern("d MMM");
	private static final DateTimeFormatter TIME_FORMATTER=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	public static final DateTimeFormatter DATE_TIME_FORMATTER=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);

	private UiUtils(){}

	public static void launchWebBrowser(Context context, String url){
		Intent intent;
		if(GlobalUserPreferences.useCustomTabs){
			intent=new CustomTabsIntent.Builder()
					.setShowTitle(true)
					.build()
					.intent;
		}else{
			intent=new Intent(Intent.ACTION_VIEW);
		}
		intent.setData(Uri.parse(url));
		ComponentName handler=intent.resolveActivity(context.getPackageManager());
		if(handler==null){
			Toast.makeText(context, R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
			return;
		}
		if(handler.getPackageName().equals(context.getPackageName())){ // Oops. Let's prevent the app from opening itself.
			ComponentName browserActivity=new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")).resolveActivity(context.getPackageManager());
			if(browserActivity==null){
				Toast.makeText(context, R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
				return;
			}
			intent.setComponent(browserActivity);
		}
		context.startActivity(intent);
	}

	public static String formatRelativeTimestamp(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<1000L){
			return context.getString(R.string.time_now);
		}else if(diff<60_000L){
			return context.getString(R.string.time_seconds_ago_short, diff/1000L);
		}else if(diff<3600_000L){
			return context.getString(R.string.time_minutes_ago_short, diff/60_000L);
		}else if(diff<3600_000L*24L){
			return context.getString(R.string.time_hours_ago_short, diff/3600_000L);
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
			return context.getString(R.string.time_days_ago_short, days);
		}
	}

	public static String formatRelativeTimestampAsMinutesAgo(Context context, Instant instant, boolean relativeHours){
		long t=instant.toEpochMilli();
		long diff=System.currentTimeMillis()-t;
		if(diff<1000L && diff>-1000L){
			return context.getString(R.string.time_just_now);
		}else if(diff>0){
			if(diff<60_000L){
				int secs=(int)(diff/1000L);
				return context.getResources().getQuantityString(R.plurals.x_seconds_ago, secs, secs);
			}else if(diff<3600_000L){
				int mins=(int)(diff/60_000L);
				return context.getResources().getQuantityString(R.plurals.x_minutes_ago, mins, mins);
			}else if(relativeHours && diff<24*3600_000L){
				int hours=(int)(diff/3600_000L);
				return context.getResources().getQuantityString(R.plurals.x_hours_ago, hours, hours);
			}
		}else{
			if(diff>-60_000L){
				int secs=-(int)(diff/1000L);
				return context.getResources().getQuantityString(R.plurals.in_x_seconds, secs, secs);
			}else if(diff>-3600_000L){
				int mins=-(int)(diff/60_000L);
				return context.getResources().getQuantityString(R.plurals.in_x_minutes, mins, mins);
			}else if(relativeHours && diff>-24*3600_000L){
				int hours=-(int)(diff/3600_000L);
				return context.getResources().getQuantityString(R.plurals.in_x_hours, hours, hours);
			}
		}
		ZonedDateTime dt=instant.atZone(ZoneId.systemDefault());
		ZonedDateTime now=ZonedDateTime.now();
		String formattedTime=TIME_FORMATTER.format(dt);
		String formattedDate;
		LocalDate today=now.toLocalDate();
		LocalDate date=dt.toLocalDate();
		if(date.equals(today)){
			formattedDate=context.getString(R.string.today);
		}else if(date.equals(today.minusDays(1))){
			formattedDate=context.getString(R.string.yesterday);
		}else if(date.equals(today.plusDays(1))){
			formattedDate=context.getString(R.string.tomorrow);
		}else if(date.getYear()==today.getYear()){
			formattedDate=DATE_FORMATTER_SHORT.format(dt);
		}else{
			formattedDate=DATE_FORMATTER_SHORT_WITH_YEAR.format(dt);
		}
		return context.getString(R.string.date_at_time, formattedDate, formattedTime);
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
			}, null, new UrlImageLoaderRequest(emoji.getKey().url, emojiSize, emojiSize), false, true);
		}
	}

	public static int getThemeColor(Context context, @AttrRes int attr){
		TypedArray ta=context.obtainStyledAttributes(new int[]{attr});
		int color=ta.getColor(0, 0xff00ff00);
		ta.recycle();
		return color;
	}

	public static void openProfileByID(Context context, String selfID, String id){
		openProfileByID(context, selfID, id, null, null);
	}

	public static void openProfileByID(Context context, String selfID, String id, String username, String domain){
		Bundle args=new Bundle();
		args.putString("account", selfID);
		args.putString("profileAccountID", id);
		if(username!=null && domain!=null){
			args.putString("accountUsername", username);
			args.putString("accountDomain", domain);
		}
		Nav.go((Activity)context, ProfileFragment.class, args);
	}

	public static void openHashtagTimeline(Context context, String accountID, Hashtag hashtag){
		if(checkIfAlreadyDisplayingSameHashtag(context, hashtag.name))
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("hashtag", Parcels.wrap(hashtag));
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
	}

	public static void openHashtagTimeline(Context context, String accountID, String hashtag){
		if(checkIfAlreadyDisplayingSameHashtag(context, hashtag))
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("hashtagName", hashtag);
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
	}

	private static boolean checkIfAlreadyDisplayingSameHashtag(Context context, String hashtag){
		if(context instanceof MainActivity ma && ma.getTopmostFragment() instanceof HashtagTimelineFragment htf && htf.getHashtagName().equalsIgnoreCase(hashtag)){
			htf.shakeListView();
			return true;
		}
		return false;
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, Runnable onConfirmed){
		showConfirmationAlert(context, context.getString(title), message==0 ? null : context.getString(message), context.getString(confirmButton), onConfirmed);
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
		if(!currentlyBlocked){
			new BlockAccountConfirmationSheet(activity, account, (onSuccess, onError)->{
				new SetAccountBlocked(account.id, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetAccountBlocked(account.id, false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							new Snackbar.Builder(activity)
									.setText(activity.getString(R.string.unblocked_user_x, account.getDisplayUsername()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.loading, false)
					.exec(accountID);
		}
	}

	public static void confirmToggleBlockDomain(Activity activity, String accountID, Account account, boolean currentlyBlocked, Runnable resultCallback, Consumer<Relationship> callbackInCaseUserWasBlockedInstead){
		if(!currentlyBlocked){
			new BlockDomainConfirmationSheet(activity, account, (onSuccess, onError)->{
				new SetDomainBlocked(account.getDomain(), true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Object result){
								resultCallback.run();
								onSuccess.run();
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}, (onSuccess, onError)->{
				new SetAccountBlocked(account.id, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								callbackInCaseUserWasBlockedInstead.accept(result);
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetDomainBlocked(account.getDomain(), false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Object result){
							resultCallback.run();
							new Snackbar.Builder(activity)
									.setText(activity.getString(R.string.unblocked_domain_x, account.getDomain()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.loading, false)
					.exec(accountID);
		}
	}

	public static void confirmToggleMuteUser(Activity activity, String accountID, Account account, boolean currentlyMuted, Consumer<Relationship> resultCallback){
		if(!currentlyMuted){
			new MuteAccountConfirmationSheet(activity, account, (onSuccess, onError)->{
				new SetAccountMuted(account.id, true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								onSuccess.run();
								E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								onError.run();
							}
						})
						.exec(accountID);
			}).show();
		}else{
			new SetAccountMuted(account.id, false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							new Snackbar.Builder(activity)
									.setText(activity.getString(R.string.unmuted_user_x, account.getDisplayUsername()))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
						}
					})
					.wrapProgress(activity, R.string.loading, false)
					.exec(accountID);
		}
	}

	public static void confirmDeletePost(Activity activity, String accountID, Status status, Consumer<Status> resultCallback){
		Runnable delete=()->new DeleteStatus(status.id)
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
		if(GlobalUserPreferences.confirmDeletePost)
			showConfirmationAlert(activity, R.string.confirm_delete_title, R.string.confirm_delete, R.string.delete, delete);
		else
			delete.run();
	}

	public static void setRelationshipToActionButtonM3(Relationship relationship, Button button){
		int styleRes;
		if(relationship.blocking){
			button.setText(R.string.button_blocked);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal_Error;
		}else if(relationship.blockedBy){
			button.setText(R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else if(relationship.requested){
			button.setText(R.string.button_follow_pending);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal;
		}else if(!relationship.following){
			button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}else{
			button.setText(R.string.button_following);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal;
		}

		button.setEnabled(!relationship.blockedBy);
		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	public static void performAccountAction(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback){
		if(relationship.blocking){
			confirmToggleBlockUser(activity, accountID, account, true, resultCallback);
		}else if(relationship.muting){
			confirmToggleMuteUser(activity, accountID, account, true, resultCallback);
		}else{
			Runnable action=()->{
				progressCallback.accept(true);
				new SetAccountFollowed(account.id, !relationship.following && !relationship.requested, true, false)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								progressCallback.accept(false);
								if(!result.following && !result.requested){
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
			};
			if(relationship.following && GlobalUserPreferences.confirmUnfollow){
				showConfirmationAlert(activity, null, activity.getString(R.string.unfollow_confirmation, account.getDisplayUsername()), activity.getString(R.string.unfollow), action);
			}else{
				action.run();
			}
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
			ssb.setSpan(new SpacerSpan(V.dp(24), 0), 0, 1, 0);
			ssb.append(" ", new SpacerSpan(V.dp(8), 0), 0);
			item.setTitle(ssb);
		}
	}

	public static void setUserPreferredTheme(Context context){
		context.setTheme(switch(GlobalUserPreferences.theme){
			case AUTO -> switch(getColorContrastMode(context)){
				case DEFAULT -> R.style.Theme_Mastodon_AutoLightDark;
				case MEDIUM -> R.style.Theme_Mastodon_AutoLightDark_MediumContrast;
				case HIGH -> R.style.Theme_Mastodon_AutoLightDark_HighContrast;
			};
			case LIGHT -> switch(getColorContrastMode(context)){
				case DEFAULT -> R.style.Theme_Mastodon_Light;
				case MEDIUM -> R.style.Theme_Mastodon_Light_MediumContrast;
				case HIGH -> R.style.Theme_Mastodon_Light_HighContrast;
			};
			case DARK -> switch(getColorContrastMode(context)){
				case DEFAULT -> R.style.Theme_Mastodon_Dark;
				case MEDIUM -> R.style.Theme_Mastodon_Dark_MediumContrast;
				case HIGH -> R.style.Theme_Mastodon_Dark_HighContrast;
			};
		});
	}

	public static boolean isDarkTheme(){
		if(GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO)
			return (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
		return GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK;
	}

	public static void openURL(Context context, String accountID, String url, Object parentObject){
		String objectURL=null;
		if(parentObject instanceof Status s){
			objectURL=s.url;
		}else if(parentObject instanceof Account a){
			objectURL=a.url;
		}
		Uri uri=Uri.parse(url);
		if(accountID!=null && "https".equals(uri.getScheme()) && !Objects.equals(url, objectURL)){
			List<String> path=uri.getPathSegments();
			if(AccountSessionManager.getInstance().getAccount(accountID).domain.equalsIgnoreCase(uri.getAuthority()) && path.size()==2 && path.get(0).matches("^@[a-zA-Z0-9_]+$") && path.get(1).matches("^[0-9]+$")){
				// Match URLs like https://mastodon.social/@Gargron/108132679274083591
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
			}else{
				new GetSearchResults(url, null, true, null, 0, 0)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(SearchResults result){
								Bundle args=new Bundle();
								args.putString("account", accountID);
								if(result.statuses!=null && !result.statuses.isEmpty()){
									Status s=result.statuses.get(0);
									if(parentObject instanceof Status status && s.id.equals(status.id)){
										launchWebBrowser(context, url);
										return;
									}
									args.putParcelable("status", Parcels.wrap(s));
									Nav.go((Activity)context, ThreadFragment.class, args);
								}else if(result.accounts!=null && !result.accounts.isEmpty()){
									Account a=result.accounts.get(0);
									if(parentObject instanceof Account account && a.id.equals(account.id)){
										launchWebBrowser(context, url);
										return;
									}
									args.putParcelable("profileAccount", Parcels.wrap(a));
									Nav.go((Activity)context, ProfileFragment.class, args);
								}else{
									launchWebBrowser(context, url);
								}
							}

							@Override
							public void onError(ErrorResponse error){
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

	public static boolean isEMUI() {
		return !TextUtils.isEmpty(getSystemProperty("ro.build.version.emui"));
	}

	public static boolean isMagic() {
		return !TextUtils.isEmpty(getSystemProperty("ro.build.version.magic"));
	}

	public static int alphaBlendColors(int color1, int color2, float alpha){
		float alpha0=1f-alpha;
		int r=Math.round(((color1 >> 16) & 0xFF)*alpha0+((color2 >> 16) & 0xFF)*alpha);
		int g=Math.round(((color1 >> 8) & 0xFF)*alpha0+((color2 >> 8) & 0xFF)*alpha);
		int b=Math.round((color1 & 0xFF)*alpha0+(color2 & 0xFF)*alpha);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static int alphaBlendThemeColors(Context context, @AttrRes int color1, @AttrRes int color2, float alpha){
		return alphaBlendColors(getThemeColor(context, color1), getThemeColor(context, color2), alpha);
	}

	/**
	 * Check to see if Android platform photopicker is available on the device\
	 *
	 * @return whether the device supports photopicker intents.
	 */
	@SuppressLint("NewApi")
	public static boolean isPhotoPickerAvailable(){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
			return true;
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
			return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)>=2;
		}else
			return false;
	}

	@SuppressLint("InlinedApi")
	public static Intent getMediaPickerIntent(String[] mimeTypes, int maxCount){
		Intent intent;
		if(isPhotoPickerAvailable()){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			if(maxCount>1)
				intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxCount);
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
		}
		if(mimeTypes.length>1){
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		}else if(mimeTypes.length==1){
			intent.setType(mimeTypes[0]);
		}else{
			intent.setType("*/*");
		}
		if(maxCount>1)
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		return intent;
	}

	/**
	 * Wraps a View.OnClickListener to filter multiple clicks in succession.
	 * Useful for buttons that perform some action that changes their state asynchronously.
	 * @param l
	 * @return
	 */
	public static View.OnClickListener rateLimitedClickListener(View.OnClickListener l){
		return new View.OnClickListener(){
			private long lastClickTime;

			@Override
			public void onClick(View v){
				if(SystemClock.uptimeMillis()-lastClickTime>500L){
					lastClickTime=SystemClock.uptimeMillis();
					l.onClick(v);
				}
			}
		};
	}

	@SuppressLint("DefaultLocale")
	public static String formatMediaDuration(int seconds){
		if(seconds>=3600)
			return String.format("%d:%02d:%02d", seconds/3600, seconds%3600/60, seconds%60);
		else
			return String.format("%d:%02d", seconds/60, seconds%60);
	}

	public static void beginLayoutTransition(ViewGroup sceneRoot){
		TransitionManager.beginDelayedTransition(sceneRoot, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.addTransition(new ChangeScroll())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
		);
	}

	public static Drawable getThemeDrawable(Context context, @AttrRes int attr){
		TypedArray ta=context.obtainStyledAttributes(new int[]{attr});
		Drawable d=ta.getDrawable(0);
		ta.recycle();
		return d;
	}

	public static WindowInsets applyBottomInsetToFixedView(View view, WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			view.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(40)) : 0);
			return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
		}
		return insets;
	}

	public static void applyBottomInsetToFAB(View fab, WindowInsets insets){
		int inset;
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0 /*&& wantsOverlaySystemNavigation()*/){
			int bottomInset=insets.getSystemWindowInsetBottom();
			inset=bottomInset>0 ? Math.max(V.dp(40), bottomInset) : 0;
		}else{
			inset=0;
		}
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+inset;
	}

	public static String formatDuration(Context context, int seconds){
		if(seconds<3600){
			int minutes=seconds/60;
			return context.getResources().getQuantityString(R.plurals.x_minutes, minutes, minutes);
		}else if(seconds<24*3600){
			int hours=seconds/3600;
			return context.getResources().getQuantityString(R.plurals.x_hours, hours, hours);
		}else if(seconds>=7*24*3600 && seconds%(7*24*3600)<24*3600){
			int weeks=seconds/(7*24*3600);
			return context.getResources().getQuantityString(R.plurals.x_weeks, weeks, weeks);
		}else{
			int days=seconds/(24*3600);
			return context.getResources().getQuantityString(R.plurals.x_days, days, days);
		}
	}

	public static Uri getFileProviderUri(Context context, File file){
		return FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", file);
	}

	public static void openSystemShareSheet(Context context, Object obj){
		Intent intent=new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		Account account;
		String url;
		String previewTitle;

		if(obj instanceof Account acc){
			account=acc;
			url=acc.url;
			previewTitle=context.getString(R.string.share_sheet_preview_profile, account.displayName);
		}else if(obj instanceof Status st){
			account=st.account;
			url=st.url;
			String postText=st.getStrippedText();
			if(TextUtils.isEmpty(postText)){
				previewTitle=context.getString(R.string.share_sheet_preview_profile, account.displayName);
			}else{
				if(postText.length()>100)
					postText=postText.substring(0, 100)+"...";
				previewTitle=context.getString(R.string.share_sheet_preview_post, account.displayName, postText);
			}
		}else{
			throw new IllegalArgumentException("Unsupported share object type");
		}

		intent.putExtra(Intent.EXTRA_TEXT, url);
		intent.putExtra(Intent.EXTRA_TITLE, previewTitle);
		ImageCache cache=ImageCache.getInstance(context);
		try{
			File ava=cache.getFile(new UrlImageLoaderRequest(account.avatarStatic));
			if(ava==null || !ava.exists())
				ava=cache.getFile(new UrlImageLoaderRequest(account.avatar));
			if(ava!=null && ava.exists()){
				intent.setClipData(ClipData.newRawUri(null, getFileProviderUri(context, ava)));
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}
		}catch(IOException ignore){}
		context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_toot_title)));
	}

	public static void maybeShowTextCopiedToast(Context context){
		//show toast, android from S_V2 on has built-in popup, as documented in
		//https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
		if(needShowClipboardToast()){
			Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
		}
	}

	public static boolean needShowClipboardToast(){
		return Build.VERSION.SDK_INT<=Build.VERSION_CODES.S_V2;
	}

	public static void setAllPaddings(View view, int paddingDp){
		int pad=V.dp(paddingDp);
		view.setPadding(pad, pad, pad, pad);
	}

	public static ViewGroup.MarginLayoutParams makeLayoutParams(int width, int height, int marginStart, int marginTop, int marginEnd, int marginBottom){
		ViewGroup.MarginLayoutParams lp=new ViewGroup.MarginLayoutParams(width>0 ? V.dp(width) : width, height>0 ? V.dp(height) : height);
		lp.topMargin=V.dp(marginTop);
		lp.bottomMargin=V.dp(marginBottom);
		lp.setMarginStart(V.dp(marginStart));
		lp.setMarginEnd(V.dp(marginEnd));
		return lp;
	}

	public static CharSequence fixBulletListInString(Context context, @StringRes int res){
		SpannableStringBuilder msg=new SpannableStringBuilder(context.getText(res));
		BulletSpan[] spans=msg.getSpans(0, msg.length(), BulletSpan.class);
		for(BulletSpan span:spans){
			BulletSpan betterSpan;
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
				betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(context, R.attr.colorM3OnSurface));
			else
				betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(context, R.attr.colorM3OnSurface), V.dp(1.5f));
			msg.setSpan(betterSpan, msg.getSpanStart(span), msg.getSpanEnd(span), msg.getSpanFlags(span));
			msg.removeSpan(span);
		}
		return msg;
	}

	public static void showProgressForAlertButton(Button button, boolean show){
		boolean shown=button.getTag(R.id.button_progress_orig_color)!=null;
		if(shown==show)
			return;
		button.setEnabled(!show);
		if(show){
			ColorStateList origColor=button.getTextColors();
			button.setTag(R.id.button_progress_orig_color, origColor);
			button.setTextColor(0);
			ProgressBar progressBar=(ProgressBar) LayoutInflater.from(button.getContext()).inflate(R.layout.progress_bar, null);
			Drawable progress=progressBar.getIndeterminateDrawable().mutate();
			progress.setTint(getThemeColor(button.getContext(), R.attr.colorM3OnSurface) & 0x60ffffff);
			if(progress instanceof Animatable a)
				a.start();
			LayerDrawable layerList=new LayerDrawable(new Drawable[]{progress});
			layerList.setLayerGravity(0, Gravity.CENTER);
			layerList.setLayerSize(0, V.dp(24), V.dp(24));
			layerList.setBounds(0, 0, button.getWidth(), button.getHeight());
			button.getOverlay().add(layerList);
		}else{
			button.getOverlay().clear();
			ColorStateList origColor=(ColorStateList) button.getTag(R.id.button_progress_orig_color);
			button.setTag(R.id.button_progress_orig_color, null);
			button.setTextColor(origColor);
		}
	}

	public static void updateRecyclerViewKeepingAbsoluteScrollPosition(RecyclerView rv, Runnable onUpdate){
		int topItem=-1;
		int topItemOffset=0;
		if(rv.getChildCount()>0){
			View item=rv.getChildAt(0);
			topItem=rv.getChildAdapterPosition(item);
			topItemOffset=item.getTop();
		}
		onUpdate.run();
		int newCount=rv.getAdapter().getItemCount();
		if(newCount>=topItem){
			rv.scrollToPosition(topItem);
			rv.scrollBy(0, -topItemOffset);
		}
	}

	public static ColorContrastMode getColorContrastMode(Context context){
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			return ColorContrastMode.DEFAULT;
		return ColorContrastMode.fromContrastValue(context.getSystemService(UiModeManager.class).getContrast());
	}

	@TargetApi(Build.VERSION_CODES.R)
	public static boolean playVibrationEffectIfSupported(Context context, int effect){
		Vibrator vibrator=context.getSystemService(Vibrator.class);
		if(vibrator.areAllPrimitivesSupported(effect)){
			vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(effect).compose());
			return true;
		}
		return false;
	}
}
