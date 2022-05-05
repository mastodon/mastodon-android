package org.joinmastodon.android.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class ImageDescriptionSheet extends BottomSheet{
	private LinearLayout layout;

	public ImageDescriptionSheet(@NonNull Activity activity, Attachment attachment){
		super(activity);

		View handleView=new View(activity);
		handleView.setBackgroundResource(R.drawable.bg_bottom_sheet_handle);
		ViewGroup handle=new FrameLayout(activity);
		handle.addView(handleView);
		handle.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(24)));

		TextView textView = new TextView(activity);
		if (attachment.description == null || attachment.description.isEmpty()) {
			textView.setText(R.string.media_no_description);
			textView.setTypeface(null, Typeface.ITALIC);
		} else {
			textView.setText(attachment.description);
		}

		TextView header = new TextView(activity);
		header.setText(R.string.image_description);
		header.setAllCaps(true);
		header.setTypeface(null, Typeface.BOLD);
		header.setPadding(0, V.dp(24), 0, V.dp(8));

		layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(header);
		layout.addView(textView);

		FrameLayout content=new FrameLayout(activity);
		content.setBackgroundResource(R.drawable.bg_bottom_sheet);
		content.addView(handle);
		content.addView(layout);
		content.setPadding(V.dp(24), V.dp(0), V.dp(24), V.dp(0));

		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground)), !UiUtils.isDarkTheme());
	}

	@Override
	protected void onWindowInsetsUpdated(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29){
			int tappableBottom=insets.getTappableElementInsets().bottom;
			int insetBottom=insets.getSystemWindowInsetBottom();
			if(tappableBottom==0 && insetBottom>0){
				layout.setPadding(0, 0, 0, V.dp(48)-insetBottom);
			}else{
				layout.setPadding(0, 0, 0, V.dp(24));
			}
		}else{
			layout.setPadding(0, 0, 0, V.dp(24));
		}
	}
}
