package org.joinmastodon.android.ui.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.ColorRes;
import androidx.browser.customtabs.CustomTabsIntent;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class UiUtils{
	private static Handler mainHandler=new Handler(Looper.getMainLooper());

	private UiUtils(){}

	public static void launchWebBrowser(Context context, String url){
		// TODO setting for custom tabs
		new CustomTabsIntent.Builder()
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
	 * @param color
	 */
	public static void fixCompoundDrawableTintOnAndroid6(TextView textView, @ColorRes int color){
		Drawable[] drawables=textView.getCompoundDrawablesRelative();
		for(int i=0;i<drawables.length;i++){
			if(drawables[i]!=null){
				Drawable tinted=drawables[i].mutate();
				tinted.setTintList(textView.getContext().getColorStateList(color));
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
}
