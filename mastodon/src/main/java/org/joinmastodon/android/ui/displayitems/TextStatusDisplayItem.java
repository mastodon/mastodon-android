package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.view.ViewGroup;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;

public class TextStatusDisplayItem extends StatusDisplayItem implements LinkSpan.OnLinkClickListener{
	private CharSequence text;
	private ImageLoaderRequest[] emojiRequests;
	private Fragment parentFragment;
	public TextStatusDisplayItem(String parentID, CharSequence text, Fragment parentFragment){
		super(parentID);
		this.text=text;
		this.parentFragment=parentFragment;
		if(text instanceof Spanned){
			CustomEmojiSpan[] emojiSpans=((Spanned) text).getSpans(0, text.length(), CustomEmojiSpan.class);
			emojiRequests=new ImageLoaderRequest[emojiSpans.length];
			int emojiSize=V.dp(20);
			for(int i=0; i<emojiSpans.length; i++){
				emojiRequests[i]=new UrlImageLoaderRequest(emojiSpans[i].emoji.url, emojiSize, emojiSize);
			}
			LinkSpan[] linkSpans=((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
			for(LinkSpan span:linkSpans){
				span.setListener(this);
			}
		}else{
			emojiRequests=new ImageLoaderRequest[0];
		}
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		return emojiRequests.length;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiRequests[index];
	}

	@Override
	public void onLinkClick(LinkSpan span){
		switch(span.getType()){
			case URL -> UiUtils.launchWebBrowser(parentFragment.getActivity(), span.getLink());
			case HASHTAG, MENTION -> Toast.makeText(parentFragment.getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
		}
	}

	public static class Holder extends BindableViewHolder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private CustomEmojiSpan[] emojiSpans;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			text.setText(item.text);
			if(emojiSpans!=null){
				for(CustomEmojiSpan span:emojiSpans){
					span.setDrawable(null);
				}
			}
			if(item.text instanceof Spanned)
				emojiSpans=((Spanned) item.text).getSpans(0, item.text.length(), CustomEmojiSpan.class);
			else
				emojiSpans=new CustomEmojiSpan[0];
			text.setInvalidateOnEveryFrame(false);
		}

		@Override
		public void setImage(int index, Drawable image){
			emojiSpans[index].setDrawable(image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
				if(image instanceof MovieDrawable)
					text.setInvalidateOnEveryFrame(true);
			}
		}

		@Override
		public void clearImage(int index){
			emojiSpans[index].setDrawable(null);
			text.invalidate();
		}
	}
}
