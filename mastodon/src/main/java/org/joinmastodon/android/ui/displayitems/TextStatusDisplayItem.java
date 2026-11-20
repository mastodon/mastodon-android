package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;

import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence translatedText;
	private CustomEmojiHelper translationEmojiHelper=new CustomEmojiHelper();
	public boolean textSelectable;
	public boolean largerFont;
	@Nullable
	public final Status status;
	private final String accountID;
	public boolean textExpanded;

	public TextStatusDisplayItem(String parentID, CharSequence text, Callbacks callbacks, Context context, @Nullable Status status, String accountID){
		super(parentID, callbacks, context);
		this.text=text;
		this.status=status;
		this.accountID=accountID;
		emojiHelper.setText(text);
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		return getCurrentEmojiHelper().getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return getCurrentEmojiHelper().getImageRequest(index);
	}

	public void setTranslatedText(String text){
		if(status==null)
			throw new IllegalStateException();
		Status statusForContent=status.getContentStatus();
		translatedText=HtmlParser.parse(text, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID, statusForContent, context);
		translationEmojiHelper.setText(translatedText);
	}

	private CustomEmojiHelper getCurrentEmojiHelper(){
		return status!=null && status.translationState==Status.TranslationState.SHOWN ? translationEmojiHelper : emojiHelper;
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final Button expandTextButton;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
			expandTextButton=findViewById(R.id.expand_button);
			expandTextButton.setOnClickListener(v->{
				item.textExpanded=true;
				getBindingAdapter().notifyItemChanged(getAbsoluteAdapterPosition());
			});
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			if(item.status!=null && item.status.translationState==Status.TranslationState.SHOWN){
				if(item.translatedText==null){
					item.setTranslatedText(item.status.translation.content);
				}
				text.setText(item.translatedText);
			}else{
				text.setText(item.text);
			}
			text.setTextIsSelectable(item.textSelectable);
			text.setInvalidateOnEveryFrame(false);
			itemView.setClickable(false);
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 0 : 48), itemView.getPaddingTop(), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			text.setTextColor(UiUtils.getThemeColor(text.getContext(), R.attr.colorM3OnSurface));
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.largerFont ? 18 : 16);
			updateTranslation(false);
			if(item.textExpanded || item.fullWidth)
				text.setMaxLines(Integer.MAX_VALUE);
			else
				text.setMaxLines(35);
		}

		@Override
		public void setImage(int index, Drawable image){
			getEmojiHelper().setImageDrawable(index, image);
			text.invalidate();
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
			return item.emojiHelper;
		}

		public void updateTranslation(boolean updateText){
			if(item.status==null)
				return;
			if(item.status.translationState==Status.TranslationState.HIDDEN){
				if(updateText){
					text.setText(item.text);
				}
			}else{
				if(item.status.translationState==Status.TranslationState.SHOWN){
					if(updateText){
						if(item.translatedText==null){
							item.setTranslatedText(item.status.translation.content);
						}
						text.setText(item.translatedText);
					}
				}
			}
		}
	}
}
