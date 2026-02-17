package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;

import java.util.Locale;

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
	public final Status status;
	private final String accountID;
	private static final int MAX_LINES = 15;

	public TextStatusDisplayItem(String parentID, CharSequence text, Callbacks callbacks, Context context, Status status, String accountID){
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
		Status statusForContent=status.getContentStatus();
		translatedText=HtmlParser.parse(text, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID, statusForContent, context);
		translationEmojiHelper.setText(translatedText);
	}

	private CustomEmojiHelper getCurrentEmojiHelper(){
		return status.translationState==Status.TranslationState.SHOWN ? translationEmojiHelper : emojiHelper;
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final TextView readMore;
		private final ViewStub translationFooterStub;
		private View translationFooter;
		private TextView translationInfo;
		private Button translationShowOriginal;
		private ProgressBar translationProgress;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
			readMore=findViewById(R.id.read_more);
			translationFooterStub=findViewById(R.id.translation_info);
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			if(item.status.translationState==Status.TranslationState.SHOWN){
				if(item.translatedText==null){
					item.setTranslatedText(item.status.translation.content);
				}
				text.setText(item.translatedText);
			}else{
				text.setText(item.text);
			}

			text.setMaxLines(Integer.MAX_VALUE);
			text.setEllipsize(null);
			readMore.setVisibility(View.GONE);

			boolean hasSpoiler=!TextUtils.isEmpty(item.status.getContentStatus().spoilerText) ||
					(item.status.filtered!=null && !item.status.filtered.isEmpty());
			if(!item.fullWidth && !(item.callbacks instanceof ThreadFragment) && !hasSpoiler){
				text.post(()->{
					if (text.getLineCount() > MAX_LINES) {
						text.setMaxLines(MAX_LINES);
						text.setEllipsize(TextUtils.TruncateAt.END);
						readMore.setVisibility(View.VISIBLE);
						readMore.setOnClickListener(v -> item.callbacks.onItemClick(item.parentID));
					}
				});
			}

			text.setTextIsSelectable(item.textSelectable);
			text.setInvalidateOnEveryFrame(false);
			itemView.setClickable(false);
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 0 : 48), 0, 0, 0);
			text.setTextColor(UiUtils.getThemeColor(text.getContext(), R.attr.colorM3OnSurface));
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.largerFont ? 18 : 16);
			updateTranslation(false);
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
				if(translationFooter!=null)
					translationFooter.setVisibility(View.GONE);
				if(updateText){
					text.setText(item.text);
				}
			}else{
				if(translationFooter==null){
					translationFooter=translationFooterStub.inflate();
					translationInfo=findViewById(R.id.translation_info_text);
					translationShowOriginal=findViewById(R.id.translation_show_original);
					translationProgress=findViewById(R.id.translation_progress);
					translationShowOriginal.setOnClickListener(v->item.callbacks.togglePostTranslation(item.status, item.parentID));
				}else{
					translationFooter.setVisibility(View.VISIBLE);
				}
				if(item.status.translationState==Status.TranslationState.SHOWN){
					translationProgress.setVisibility(View.GONE);
					translationInfo.setVisibility(View.VISIBLE);
					translationShowOriginal.setVisibility(View.VISIBLE);
					translationInfo.setText(translationInfo.getContext().getString(R.string.post_translated, Locale.forLanguageTag(item.status.translation.detectedSourceLanguage).getDisplayLanguage(), item.status.translation.provider));
					if(updateText){
						if(item.translatedText==null){
							item.setTranslatedText(item.status.translation.content);
						}
						text.setText(item.translatedText);
					}
				}else{ // LOADING
					translationProgress.setVisibility(View.VISIBLE);
					translationInfo.setVisibility(View.INVISIBLE);
					translationShowOriginal.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}
