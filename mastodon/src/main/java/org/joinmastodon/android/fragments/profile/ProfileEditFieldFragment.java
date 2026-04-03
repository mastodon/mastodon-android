package org.joinmastodon.android.fragments.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toolbar;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.requests.tags.RemoveFeaturedHashtag;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.OwnFeaturedHashtagRemovedEvent;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class ProfileEditFieldFragment extends MastodonToolbarFragment{
	private String accountID;
	private boolean isExistingField;
	private EditText labelEdit, valueEdit;
	private Button saveButton;
	private boolean buttonEnabled=false;
	private ArrayList<AccountField> fields=new ArrayList<>();
	private int fieldIndex;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		for(Parcelable p: Objects.requireNonNull(getArguments().getParcelableArrayList("fields"))){
			fields.add(Parcels.unwrap(p));
		}
		fieldIndex=getArguments().getInt("fieldIndex", -1);
		isExistingField=fieldIndex!=-1;
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
			labelEdit.setText(fields.get(fieldIndex).name);
			valueEdit.setText(fields.get(fieldIndex).value);
		}else{
			deleteBtn.setVisibility(View.GONE);
		}

		labelEdit.addTextChangedListener(new SimpleTextWatcher(text->saveButton.setEnabled(buttonEnabled=text.length()>0)));
		valueEdit.addTextChangedListener(new SimpleTextWatcher(text->saveButton.setEnabled(buttonEnabled=text.length()>0)));

		saveButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_M3_Button_Filled);
		saveButton.setText(R.string.save);
		saveButton.setEnabled(buttonEnabled);
		int pad=V.dp(16);
		saveButton.setPadding(pad, 0, pad, 0);
		saveButton.setOnClickListener(v->save());

		deleteBtn.setOnClickListener(v_->{
			AlertDialog alert=new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.confirm_delete_profile_field)
					.setPositiveButton(R.string.delete, null)
					.setNegativeButton(R.string.cancel, null)
					.create();
			Button positiveBtn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
			Button negativeBtn=alert.getButton(DialogInterface.BUTTON_NEGATIVE);
			positiveBtn.setOnClickListener(v->{
				alert.setCancelable(false);
				UiUtils.showProgressForAlertButton(positiveBtn, true);
				negativeBtn.setEnabled(false);

				ArrayList<AccountField> newFields=new ArrayList<>(fields);
				newFields.remove(fieldIndex);

				new UpdateAccountCredentials(null, null, (File)null, null, newFields)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Account result){
								alert.dismiss();
								AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
								Nav.finish(ProfileEditFieldFragment.this);
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(getActivity());
								alert.setCancelable(true);
								UiUtils.showProgressForAlertButton(positiveBtn, false);
								negativeBtn.setEnabled(true);
							}
						})
						.exec(accountID);
			});
			alert.show();
		});

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
		List<AccountField> fields;
		if(isExistingField){
			fields=this.fields;
			AccountField field=fields.get(fieldIndex);
			field.name=labelEdit.getText().toString();
			field.value=valueEdit.getText().toString();
		}else{
			fields=new ArrayList<>(this.fields);
			AccountField field=new AccountField();
			field.name=labelEdit.getText().toString();
			field.value=valueEdit.getText().toString();
			fields.add(field);
		}

		new UpdateAccountCredentials(null, null, (File)null, null, fields)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						Nav.finish(ProfileEditFieldFragment.this);
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
