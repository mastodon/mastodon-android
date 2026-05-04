package org.joinmastodon.android.fragments.profile;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
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
import org.joinmastodon.android.api.requests.tags.AddFeaturedHashtag;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.OwnFeaturedHashtagAddedEvent;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class ProfileEditAddHashtagFragment extends MastodonToolbarFragment{
	private EditText edit;
	private Button saveButton;
	private boolean buttonEnabled=false;
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.add_featured_hashtag);
		setRetainInstance(true);
		accountID=getArguments().getString("account");
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		FloatingHintEditTextLayout editWrap=(FloatingHintEditTextLayout) inflater.inflate(R.layout.floating_hint_edit_text, container, false);
		edit=editWrap.findViewById(R.id.edit);
		edit.setHint(R.string.hashtag);
		edit.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		edit.addTextChangedListener(new SimpleTextWatcher(text->saveButton.setEnabled(buttonEnabled=text.length()>0)));
		edit.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search_24px, 0, 0, 0);
		edit.setCompoundDrawablePadding(V.dp(16));
		edit.setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant)));
		editWrap.updateHint();

		saveButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_M3_Button_Filled);
		saveButton.setText(R.string.add);
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
		new AddFeaturedHashtag(edit.getText().toString())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag result){
						E.post(new OwnFeaturedHashtagAddedEvent(accountID, result));
						Nav.finish(ProfileEditAddHashtagFragment.this);
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
