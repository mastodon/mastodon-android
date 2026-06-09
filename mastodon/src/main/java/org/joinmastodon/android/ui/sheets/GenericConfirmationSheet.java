package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.views.ProgressBarButton;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class GenericConfirmationSheet extends M3BottomSheet{
	private TextView titleView, contentView;
	private ProgressBarButton primaryButton, secondaryButton;
	private Button cancelButton;
	private View primaryButtonWrap, secondaryButtonWrap;
	private ProgressBar primaryProgress, secondaryProgress;
	private boolean locked;

	public GenericConfirmationSheet(@NonNull Context context){
		super(context);
		setContentView(R.layout.sheet_generic_confirmation);

		titleView=findViewById(R.id.title);
		contentView=findViewById(R.id.content);
		primaryButton=findViewById(R.id.btn_confirm);
		secondaryButton=findViewById(R.id.btn_secondary);
		cancelButton=findViewById(R.id.btn_cancel);
		primaryButtonWrap=findViewById(R.id.btn_confirm_wrap);
		secondaryButtonWrap=findViewById(R.id.btn_secondary_wrap);
		primaryProgress=findViewById(R.id.confirm_progress);
		secondaryProgress=findViewById(R.id.secondary_progress);

		secondaryButtonWrap.setVisibility(View.GONE);

		cancelButton.setOnClickListener(v->{
			if(!locked)
				dismiss();
		});
	}

	public GenericConfirmationSheet setTitleText(CharSequence title){
		titleView.setText(title);
		return this;
	}

	public GenericConfirmationSheet setTitleText(@StringRes int res){
		titleView.setText(res);
		return this;
	}

	public GenericConfirmationSheet setContentText(CharSequence content){
		contentView.setText(content);
		return this;
	}

	public GenericConfirmationSheet setContentText(@StringRes int res){
		contentView.setText(res);
		return this;
	}

	public GenericConfirmationSheet setPrimaryButton(CharSequence text, Runnable onClick){
		primaryButton.setText(text);
		setButtonListener(primaryButton, onClick);
		return this;
	}

	public GenericConfirmationSheet setPrimaryButton(@StringRes int res, Runnable onClick){
		primaryButton.setText(res);
		setButtonListener(primaryButton, onClick);
		return this;
	}

	private void setButtonListener(Button button, Runnable onClick){
		button.setOnClickListener(onClick==null ? null : v->{
			if(!locked)
				onClick.run();
		});
	}

	public void setPrimaryButtonProgressVisible(boolean visible){
		if(locked && visible)
			throw new IllegalStateException("Progress already visible");
		locked=visible;
		if(visible){
			primaryProgress.setIndeterminateTintList(primaryButton.getTextColors());
			primaryProgress.setVisibility(View.VISIBLE);
			primaryButton.setTextVisible(false);
			setCancelable(false);
		}else{
			primaryProgress.setVisibility(View.GONE);
			primaryButton.setTextVisible(true);
			setCancelable(true);
		}
	}
}
