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
import androidx.recyclerview.widget.LinearLayoutManager;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;
import me.grishka.appkit.views.UsableRecyclerView;

public class ImageDescriptionSheet extends BottomSheet{
	private UsableRecyclerView list;

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
			textView.setTextIsSelectable(true);
		}

		TextView heading=new TextView(activity);
		heading.setText(R.string.image_description);
		heading.setAllCaps(true);
		heading.setTypeface(null, Typeface.BOLD);
		heading.setPadding(0, V.dp(24), 0, V.dp(8));

		LinearLayout linearLayout = new LinearLayout(activity);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setPadding(V.dp(24), 0, V.dp(24), 0);
		linearLayout.addView(heading);
		linearLayout.addView(textView);

		FrameLayout layout=new FrameLayout(activity);
		layout.addView(handle);
		layout.addView(linearLayout);

		list=new UsableRecyclerView(activity);
		list.setLayoutManager(new LinearLayoutManager(activity));
		list.setBackgroundResource(R.drawable.bg_bottom_sheet);
		list.setAdapter(new SingleViewRecyclerAdapter(layout));
		list.setClipToPadding(false);

		setContentView(list);
		setNavigationBarBackground(new ColorDrawable(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground)), !UiUtils.isDarkTheme());
	}

	@Override
	protected void onWindowInsetsUpdated(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29){
			int tappableBottom=insets.getTappableElementInsets().bottom;
			int insetBottom=insets.getSystemWindowInsetBottom();
			if(tappableBottom==0 && insetBottom>0){
				list.setPadding(0, 0, 0, V.dp(48)-insetBottom);
			}else{
				list.setPadding(0, 0, 0, V.dp(24));
			}
		}else{
			list.setPadding(0, 0, 0, V.dp(24));
		}
	}
}
