package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.ui.views.LinkedTextView;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private Fragment parentFragment;
	public TextStatusDisplayItem(String parentID, CharSequence text, BaseStatusListFragment parentFragment){
		super(parentID, parentFragment);
		this.text=text;
		this.parentFragment=parentFragment;
		emojiHelper.setText(text);
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			text.setText(item.text);
			text.setInvalidateOnEveryFrame(false);
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
				if(image instanceof MovieDrawable)
					text.setInvalidateOnEveryFrame(true);
			}
		}

		@Override
		public void clearImage(int index){
			item.emojiHelper.setImageDrawable(index, null);
			text.invalidate();
		}
	}
}
