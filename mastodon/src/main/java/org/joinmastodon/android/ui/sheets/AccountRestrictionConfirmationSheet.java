package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public abstract class AccountRestrictionConfirmationSheet extends BottomSheet{
	private LinearLayout contentWrap;
	protected Button cancelBtn;
	protected ProgressBarButton confirmBtn, secondaryBtn;
	protected TextView titleView, subtitleView;
	protected ImageView icon;
	protected boolean loading;

	public AccountRestrictionConfirmationSheet(@NonNull Context context, Account user, ConfirmCallback confirmCallback){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_restrict_account, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		contentWrap=findViewById(R.id.content_wrap);
		titleView=findViewById(R.id.title);
		subtitleView=findViewById(R.id.text);
		cancelBtn=findViewById(R.id.btn_cancel);
		confirmBtn=findViewById(R.id.btn_confirm);
		secondaryBtn=findViewById(R.id.btn_secondary);
		icon=findViewById(R.id.icon);

		contentWrap.setDividerDrawable(new EmptyDrawable(1, V.dp(8)));
		contentWrap.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		confirmBtn.setOnClickListener(v->{
			if(loading)
				return;
			loading=true;
			confirmBtn.setProgressBarVisible(true);
			confirmCallback.onConfirmed(this::dismiss, ()->{
				confirmBtn.setProgressBarVisible(false);
				loading=false;
			});
		});
		cancelBtn.setOnClickListener(v->{
			if(!loading)
				dismiss();
		});
	}

	protected void addRow(@DrawableRes int icon, CharSequence text){
		TextView tv=new TextView(getContext());
		tv.setTextAppearance(R.style.m3_body_large);
		tv.setTextColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3OnSurfaceVariant));
		tv.setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getContext(), R.attr.colorM3Primary)));
		tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		tv.setText(text);
		InsetDrawable drawable=new InsetDrawable(getContext().getResources().getDrawable(icon, getContext().getTheme()), V.dp(8));
		drawable.setBounds(0, 0, V.dp(40), V.dp(40));
		tv.setCompoundDrawablesRelative(drawable, null, null, null);
		tv.setCompoundDrawablePadding(V.dp(16));
		contentWrap.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	protected void addRow(@DrawableRes int icon, @StringRes int text){
		addRow(icon, getContext().getString(text));
	}

	public interface ConfirmCallback{
		void onConfirmed(Runnable onSuccess, Runnable onError);
	}
}
