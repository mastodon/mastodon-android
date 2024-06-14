package org.joinmastodon.android.ui.sheets;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.Nav;
import me.grishka.appkit.views.BottomSheet;

public class DonationSuccessfulSheet extends BottomSheet{

	public DonationSuccessfulSheet(@NonNull Context context, @NonNull String accountID, String postText){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_donation_success, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		content.findViewById(R.id.btn_done).setOnClickListener(v->dismiss());
		View shareButton=content.findViewById(R.id.btn_share);
		if(postText==null){
			shareButton.setEnabled(false);
		}
		shareButton.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putString("prefilledText", postText);
			Nav.go((Activity) context, ComposeFragment.class, args);
			dismiss();
		});
	}
}
