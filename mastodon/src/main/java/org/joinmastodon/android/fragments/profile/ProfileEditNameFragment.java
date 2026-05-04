package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
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

public class ProfileEditNameFragment extends MastodonToolbarFragment{
	private EditText edit;
	private Button saveButton;
	private boolean buttonEnabled=false;
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.edit_profile_edit_display_name);
		setRetainInstance(true);
		accountID=getArguments().getString("account");
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		FloatingHintEditTextLayout editWrap=(FloatingHintEditTextLayout) inflater.inflate(R.layout.floating_hint_edit_text, container, false);
		edit=editWrap.findViewById(R.id.edit);
		edit.setHint(R.string.edit_profile_display_name);
		edit.setText(getArguments().getString("name"));
		edit.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
		edit.addTextChangedListener(new SimpleTextWatcher(text->saveButton.setEnabled(buttonEnabled=text.length()>0)));
		editWrap.updateHint();
		editWrap.setErrorTextAsDescription(getResources().getQuantityString(R.plurals.x_chars_max, 30, 30));

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
		new UpdateAccountCredentials(edit.getText().toString(), null, (File)null, null, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						Nav.finish(ProfileEditNameFragment.this);
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
