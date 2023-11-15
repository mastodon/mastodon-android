package org.joinmastodon.android.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.views.BottomSheet;

public abstract class PreReplySheet extends BottomSheet{
	protected ImageView icon;
	protected TextView title, text;
	protected Button gotItButton, dontRemindButton;
	protected LinearLayout contentWrap;

	public PreReplySheet(@NonNull Context context, ResultListener resultListener){
		super(context);

		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_pre_reply, null);
		setContentView(content);

		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		icon=findViewById(R.id.icon);
		title=findViewById(R.id.title);
		text=findViewById(R.id.text);
		gotItButton=findViewById(R.id.btn_got_it);
		dontRemindButton=findViewById(R.id.btn_dont_remind_again);
		contentWrap=findViewById(R.id.content_wrap);

		gotItButton.setOnClickListener(v->{
			dismiss();
			resultListener.onButtonClicked(false);
		});
		dontRemindButton.setOnClickListener(v->{
			dismiss();
			resultListener.onButtonClicked(true);
		});
	}

	@FunctionalInterface
	public interface ResultListener{
		void onButtonClicked(boolean notAgain);
	}
}
