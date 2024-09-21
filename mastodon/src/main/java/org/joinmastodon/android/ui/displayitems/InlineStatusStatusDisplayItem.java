package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class InlineStatusStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	private SpannableStringBuilder parsedPostText;
	private UrlImageLoaderRequest avaRequest;
	public boolean removeTopPadding;

	public InlineStatusStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, Status status){
		super(parentID, parentFragment);
		this.status=status;

		parsedName=new SpannableStringBuilder(status.account.displayName);
		if(AccountSessionManager.get(parentFragment.getAccountID()).getLocalPreferences().customEmojiInNames)
			HtmlParser.parseCustomEmoji(parsedName, status.account.emojis);

		parsedPostText=HtmlParser.parse(status.content, status.emojis, status.mentions, status.tags, parentFragment.getAccountID(), status.getContentStatus());
		for(Object span:parsedPostText.getSpans(0, parsedPostText.length(), Object.class)){
			if(!(span instanceof CustomEmojiSpan))
				parsedPostText.removeSpan(span);
		}

		SpannableStringBuilder ssb=new SpannableStringBuilder(parsedName);
		ssb.append(parsedPostText);
		emojiHelper.setText(ssb);

		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? status.account.avatar : status.account.avatarStatic, V.dp(50), V.dp(50));
	}

	@Override
	public Type getType(){
		return Type.INLINE_STATUS;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount()+1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index==0)
			return avaRequest;
		return emojiHelper.getImageRequest(index-1);
	}

	public static class Holder extends StatusDisplayItem.Holder<InlineStatusStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, username, text;
		private final ImageView ava;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_inline_status, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			text=findViewById(R.id.text);
			ava=findViewById(R.id.ava);

			ava.setOutlineProvider(OutlineProviders.roundedRect(4));
			ava.setClipToOutline(true);
		}

		@Override
		public void onBind(InlineStatusStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), item.removeTopPadding ? 0 : V.dp(8), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			name.setText(item.parsedName);
			username.setText(item.status.account.getDisplayUsername());
			if(item.parsedPostText.length()==0){
				text.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Outline));
				text.setText(itemView.getContext().getResources().getQuantityString(R.plurals.x_attachments, item.status.mediaAttachments.size(), item.status.mediaAttachments.size()));
				Drawable icon=itemView.getContext().getDrawable(R.drawable.ic_photo_library_wght700_20px);
				icon.setBounds(0, 0, V.dp(16), V.dp(16));
				text.setCompoundDrawablesRelative(icon, null, null, null);
			}else{
				text.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurfaceVariant));
				text.setCompoundDrawables(null, null, null, null);
				text.setText(item.parsedPostText);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				ava.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				text.invalidate();
				name.invalidate();
			}
		}
	}
}
