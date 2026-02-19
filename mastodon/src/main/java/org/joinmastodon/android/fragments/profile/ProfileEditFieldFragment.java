package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;

public class ProfileEditFieldFragment extends MastodonToolbarFragment{
	private String accountID;
	private boolean isExistingField;
	private EditText labelEdit, valueEdit;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		isExistingField=getArguments().containsKey("label");
		setTitle(isExistingField ? R.string.edit_profile_field : R.string.add_profile_field);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile_edit_field, container, false);
		labelEdit=content.findViewById(R.id.label);
		valueEdit=content.findViewById(R.id.value);
		Button deleteBtn=content.findViewById(R.id.delete);

		FloatingHintEditTextLayout labelWrap=content.findViewById(R.id.label_wrap);
		labelWrap.setErrorTextAsDescription(getString(R.string.edit_profile_field_label_example));
		FloatingHintEditTextLayout valueWrap=content.findViewById(R.id.value_wrap);
		valueWrap.setErrorTextAsDescription(getString(R.string.edit_profile_field_value_example));

		if(isExistingField){
			labelEdit.setText(getArguments().getString("label"));
			valueEdit.setText(getArguments().getString("value"));
		}else{
			deleteBtn.setVisibility(View.GONE);
		}

		return content;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.postDelayed(()->{
			labelEdit.requestFocus();
			labelEdit.setSelection(labelEdit.length());
			view.getContext().getSystemService(InputMethodManager.class).showSoftInput(labelEdit, 0);
		}, 100);
	}
}
