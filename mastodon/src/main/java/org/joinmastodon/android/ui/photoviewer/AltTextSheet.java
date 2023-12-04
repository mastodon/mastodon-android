package org.joinmastodon.android.ui.photoviewer;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.views.BottomSheet;

public class AltTextSheet extends BottomSheet{
	public AltTextSheet(@NonNull Context context, Attachment attachment){
		super(context);

		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_alt_text, null);
		setContentView(content);
		TextView altText=findViewById(R.id.alt_text);
		altText.setText(attachment.description);
		findViewById(R.id.alt_text_help).setOnClickListener(v->showAltTextHelp());
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());
	}

	private void showAltTextHelp(){
		new M3AlertDialogBuilder(getContext())
				.setTitle(R.string.what_is_alt_text)
				.setMessage(UiUtils.fixBulletListInString(getContext(), R.string.alt_text_help))
				.setPositiveButton(R.string.ok, null)
				.show();
	}
}
