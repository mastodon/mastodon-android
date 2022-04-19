package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.UpdateAttachment;
import org.joinmastodon.android.model.Attachment;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeImageDescriptionFragment extends MastodonToolbarFragment{
	private String accountID, attachmentID;
	private EditText edit;
	private Button saveButton;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		attachmentID=getArguments().getString("attachment");
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.edit_image);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_image_description, container, false);

		edit=view.findViewById(R.id.edit);
		ImageView image=view.findViewById(R.id.photo);
		Uri uri=getArguments().getParcelable("uri");
		ViewImageLoader.load(image, null, new UrlImageLoaderRequest(uri, 1000, 1000));
		edit.setText(getArguments().getString("existingDescription"));

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		edit.requestFocus();
		view.postDelayed(()->getActivity().getSystemService(InputMethodManager.class).showSoftInput(edit, 0), 100);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		TypedArray ta=getActivity().obtainStyledAttributes(new int[]{R.attr.secondaryButtonStyle});
		int buttonStyle=ta.getResourceId(0, 0);
		ta.recycle();
		saveButton=new Button(getActivity(), null, 0, buttonStyle);
		saveButton.setText(R.string.save);
		saveButton.setOnClickListener(this::onSaveClick);
		FrameLayout wrap=new FrameLayout(getActivity());
		wrap.addView(saveButton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));
		wrap.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(8));
		wrap.setClipToPadding(false);
		MenuItem item=menu.add(R.string.publish);
		item.setActionView(wrap);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		return true;
	}

	private void onSaveClick(View v){
		new UpdateAttachment(attachmentID, edit.getText().toString().trim())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						Bundle r=new Bundle();
						r.putParcelable("attachment", Parcels.wrap(result));
						setResult(true, r);
						Nav.finish(ComposeImageDescriptionFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.saving, false)
				.exec(accountID);
	}
}
