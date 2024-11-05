package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

import androidx.annotation.DrawableRes;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ReblogOrReplyLineStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	@DrawableRes
	private int icon;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();

	public ReblogOrReplyLineStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, CharSequence text, List<Emoji> emojis, @DrawableRes int icon){
		super(parentID, parentFragment);
		SpannableStringBuilder ssb=new SpannableStringBuilder(text);
		if(GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(ssb, emojis);
		this.text=ssb;
		emojiHelper.setText(ssb);
		this.icon=icon;
	}

	@Override
	public Type getType(){
		return Type.REBLOG_OR_REPLY_LINE;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<ReblogOrReplyLineStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reblog_or_reply_line, parent);
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(ReblogOrReplyLineStatusDisplayItem item){
			text.setText(item.text);
			if(item.icon!=0){
				Drawable icon=itemView.getContext().getDrawable(item.icon);
				icon.setBounds(0, 0, V.dp(16), V.dp(16));
				text.setCompoundDrawablesRelative(icon, null, null, null);
				if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N)
					UiUtils.fixCompoundDrawableTintOnAndroid6(text);
			}else{
				text.setCompoundDrawables(null, null, null, null);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
