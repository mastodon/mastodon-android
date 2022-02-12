package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.ui.text.HtmlParser;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class PollOptionStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private Poll.Option option;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();

	public PollOptionStatusDisplayItem(String parentID, Poll poll, Poll.Option option, BaseStatusListFragment parentFragment){
		super(parentID, parentFragment);
		this.option=option;
		text=HtmlParser.parseCustomEmoji(option.title, poll.emojis);
		emojiHelper.setText(text);
	}

	@Override
	public Type getType(){
		return Type.POLL_OPTION;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<PollOptionStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text;
		private final View button;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_poll_option, parent);
			text=findViewById(R.id.text);
			button=findViewById(R.id.button);
			itemView.setOnClickListener(this::onButtonClick);
		}

		@Override
		public void onBind(PollOptionStatusDisplayItem item){
			text.setText(item.text);
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
			}
		}

		@Override
		public void clearImage(int index){
			item.emojiHelper.setImageDrawable(index, null);
			text.invalidate();
		}

		private void onButtonClick(View v){
			item.parentFragment.onPollOptionClick(this);
		}
	}
}
