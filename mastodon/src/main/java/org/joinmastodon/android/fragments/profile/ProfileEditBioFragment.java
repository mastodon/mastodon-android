package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;

import java.io.File;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class ProfileEditBioFragment extends MastodonToolbarFragment{
	private EditText edit;
	private Button saveButton;
	private boolean buttonEnabled=false;
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		String bio=getArguments().getString("bio");
		setTitle(TextUtils.isEmpty(bio) ? R.string.edit_profile_add_bio : R.string.edit_profile_edit_bio);
		setRetainInstance(true);
		accountID=getArguments().getString("account");
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		FloatingHintEditTextLayout editWrap=(FloatingHintEditTextLayout) inflater.inflate(R.layout.floating_hint_edit_text, container, false);
		edit=editWrap.findViewById(R.id.edit);
		edit.setHint(R.string.profile_bio);
		edit.setText(getArguments().getString("bio"));
		edit.setSingleLine(false);
		edit.addTextChangedListener(new SimpleTextWatcher(text->saveButton.setEnabled(buttonEnabled=true)));
		editWrap.updateHint();

		saveButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_M3_Button_Filled);
		saveButton.setText(R.string.save);
		saveButton.setEnabled(buttonEnabled);
		int pad=V.dp(16);
		saveButton.setPadding(pad, 0, pad, 0);
		saveButton.setOnClickListener(v->save());

		return editWrap;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.postDelayed(()->{
			edit.requestFocus();
			edit.setSelection(edit.length());
			view.getContext().getSystemService(InputMethodManager.class).showSoftInput(edit, 0);
		}, 100);
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(saveButton.getParent() instanceof ViewGroup existingParent)
			existingParent.removeView(saveButton);
		Toolbar toolbar=getToolbar();
		Toolbar.LayoutParams lp=new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END|Gravity.CENTER_VERTICAL);
		lp.setMarginEnd(V.dp(4));
		toolbar.addView(saveButton, lp);
	}

	private void save(){
		new UpdateAccountCredentials(null, edit.getText().toString(), (File)null, null, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						Nav.finish(ProfileEditBioFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.saving, true)
				.exec(accountID);
	}
}
