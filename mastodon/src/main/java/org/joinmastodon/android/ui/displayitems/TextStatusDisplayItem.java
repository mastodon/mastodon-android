package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.views.LinkedTextView;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper(), spoilerEmojiHelper;
	private CharSequence parsedSpoilerText;
	public boolean textSelectable;
	public final Status status;

	public TextStatusDisplayItem(String parentID, CharSequence text, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.text=text;
		this.status=status;
		emojiHelper.setText(text);
		if(!TextUtils.isEmpty(status.spoilerText)){
			parsedSpoilerText=HtmlParser.parseCustomEmoji(status.spoilerText, status.emojis);
			spoilerEmojiHelper=new CustomEmojiHelper();
			spoilerEmojiHelper.setText(parsedSpoilerText);
		}
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		if(spoilerEmojiHelper!=null && !status.spoilerRevealed)
			return spoilerEmojiHelper.getImageCount();
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(spoilerEmojiHelper!=null && !status.spoilerRevealed)
			return spoilerEmojiHelper.getImageRequest(index);
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final TextView spoilerTitle;
		private final View spoilerOverlay;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
			spoilerTitle=findViewById(R.id.spoiler_title);
			spoilerOverlay=findViewById(R.id.spoiler_overlay);
			itemView.setOnClickListener(v->item.parentFragment.onRevealSpoilerClick(this));
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			text.setText(item.text);
			text.setTextIsSelectable(item.textSelectable);
			text.setInvalidateOnEveryFrame(false);
			if(!TextUtils.isEmpty(item.status.spoilerText)){
				spoilerTitle.setText(item.parsedSpoilerText);
				if(item.status.spoilerRevealed){
					spoilerOverlay.setVisibility(View.GONE);
					text.setVisibility(View.VISIBLE);
					itemView.setClickable(false);
				}else{
					spoilerOverlay.setVisibility(View.VISIBLE);
					text.setVisibility(View.INVISIBLE);
					itemView.setClickable(true);
				}
			}else{
				spoilerOverlay.setVisibility(View.GONE);
				text.setVisibility(View.VISIBLE);
				itemView.setClickable(false);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			getEmojiHelper().setImageDrawable(index, image);
			text.invalidate();
			spoilerTitle.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
				if(image instanceof MovieDrawable)
					text.setInvalidateOnEveryFrame(true);
			}
		}

		@Override
		public void clearImage(int index){
			getEmojiHelper().setImageDrawable(index, null);
			text.invalidate();
		}

		private CustomEmojiHelper getEmojiHelper(){
			return item.spoilerEmojiHelper!=null && !item.status.spoilerRevealed ? item.spoilerEmojiHelper : item.emojiHelper;
		}
	}
}
