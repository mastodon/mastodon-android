package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;

import java.util.Locale;

import me.grishka.appkit.utils.V;

public class TranslateButtonStatusDisplayItem extends StatusDisplayItem{
	private final Status status;

	public TranslateButtonStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status){
		super(parentID, callbacks, context);
		this.status=status;
	}

	@Override
	public Type getType(){
		return Type.TRANSLATE_BUTTON;
	}

	public static class Holder extends StatusDisplayItem.Holder<TranslateButtonStatusDisplayItem>{
		private final Button button;
		private final TextView translationInfo;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_translate_button, parent);
			button=findViewById(R.id.button);
			button.setOnClickListener(v->item.callbacks.togglePostTranslation(item.status, item.parentID));
			translationInfo=findViewById(R.id.translation_info);
		}

		@Override
		public void onBind(TranslateButtonStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, V.dp(16), 0);
			switch(item.status.translationState){
				case SHOWN -> {
					button.setText(R.string.translation_show_original);
					button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_phosphor_language_20px, 0, 0, 0);
					button.setEnabled(true);
					translationInfo.setVisibility(View.VISIBLE);
					translationInfo.setText(item.context.getString(R.string.post_translated, Locale.forLanguageTag(item.status.translation.detectedSourceLanguage).getDisplayLanguage(), item.status.translation.provider));
				}
				case HIDDEN -> {
					button.setText(R.string.translate_post);
					button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_phosphor_language_20px, 0, 0, 0);
					button.setEnabled(true);
					translationInfo.setVisibility(View.GONE);
				}
				case LOADING -> {
					button.setText(R.string.translate_post);
					button.setEnabled(false);
					ProgressBar progressBar=(ProgressBar) LayoutInflater.from(button.getContext()).inflate(R.layout.progress_bar, null);
					Drawable progressDrawable=progressBar.getIndeterminateDrawable().mutate();
					progressDrawable.setBounds(0, 0, V.dp(20), V.dp(20));
					if(progressDrawable instanceof Animatable a)
						a.start();
					button.setCompoundDrawablesRelative(progressDrawable, null, null, null);
					translationInfo.setVisibility(View.GONE);
				}
			}
		}
	}
}
