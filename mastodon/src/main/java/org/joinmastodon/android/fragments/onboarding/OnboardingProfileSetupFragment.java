package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.ArrayList;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class OnboardingProfileSetupFragment extends ToolbarFragment implements ReorderableLinearLayout.OnDragListener{
	private Button btn;
	private View buttonBar;
	private String accountID;
	private ElevationOnScrollListener onScrollListener;
	private ScrollView scroller;
	private EditText nameEdit, bioEdit;
	private ImageView avaImage, coverImage;
	private Button addRow;
	private ReorderableLinearLayout profileFieldsLayout;
	private Uri avatarUri, coverUri;

	private static final int AVATAR_RESULT=348;
	private static final int COVER_RESULT=183;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		accountID=getArguments().getString("account");
		setTitle(R.string.profile_setup);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_profile_setup, container, false);

		scroller=view.findViewById(R.id.scroller);
		nameEdit=view.findViewById(R.id.display_name);
		bioEdit=view.findViewById(R.id.bio);
		avaImage=view.findViewById(R.id.avatar);
		coverImage=view.findViewById(R.id.header);
		addRow=view.findViewById(R.id.add_row);
		profileFieldsLayout=view.findViewById(R.id.profile_fields);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		avaImage.setOutlineProvider(OutlineProviders.roundedRect(24));
		avaImage.setClipToOutline(true);

		Account account=AccountSessionManager.getInstance().getAccount(accountID).self;
		if(savedInstanceState==null){
			nameEdit.setText(account.displayName);
			makeFieldsRow();
		}else{
			ArrayList<String> fieldTitles=savedInstanceState.getStringArrayList("fieldTitles");
			ArrayList<String> fieldValues=savedInstanceState.getStringArrayList("fieldValues");
			for(int i=0;i<fieldTitles.size();i++){
				View row=makeFieldsRow();
				EditText title=row.findViewById(R.id.title);
				EditText content=row.findViewById(R.id.content);
				title.setText(fieldTitles.get(i));
				content.setText(fieldValues.get(i));
			}
			if(fieldTitles.size()==4)
				addRow.setVisibility(View.GONE);
		}

		addRow.setOnClickListener(v->{
			makeFieldsRow();
			if(profileFieldsLayout.getChildCount()==4){
				addRow.setVisibility(View.GONE);
			}
		});
		profileFieldsLayout.setDragListener(this);
		avaImage.setOnClickListener(v->startActivityForResult(UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1), AVATAR_RESULT));
		coverImage.setOnClickListener(v->startActivityForResult(UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1), COVER_RESULT));

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		scroller.setOnScrollChangeListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	protected void onButtonClick(){
		ArrayList<AccountField> fields=new ArrayList<>();
		for(int i=0;i<profileFieldsLayout.getChildCount();i++){
			View row=profileFieldsLayout.getChildAt(i);
			EditText title=row.findViewById(R.id.title);
			EditText content=row.findViewById(R.id.content);
			AccountField fld=new AccountField();
			fld.name=title.getText().toString();
			fld.value=content.getText().toString();
			fields.add(fld);
		}
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), avatarUri, coverUri, fields)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						Bundle args=new Bundle();
						args.putString("account", accountID);
						Nav.goClearingStack(getActivity(), HomeFragment.class, args);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.saving, true)
				.exec(accountID);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			buttonBar.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
	}

	private View makeFieldsRow(){
		View view=LayoutInflater.from(getActivity()).inflate(R.layout.onboarding_profile_field, profileFieldsLayout, false);
		profileFieldsLayout.addView(view);
		view.findViewById(R.id.dragger_thingy).setOnLongClickListener(v->{
			profileFieldsLayout.startDragging(view);
			return true;
		});
		view.findViewById(R.id.delete).setOnClickListener(v->{
			profileFieldsLayout.removeView(view);
			if(addRow.getVisibility()==View.GONE)
				addRow.setVisibility(View.VISIBLE);
		});
		return view;
	}

	@Override
	public void onSwapItems(int oldIndex, int newIndex){}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		ArrayList<String> fieldTitles=new ArrayList<>(), fieldValues=new ArrayList<>();
		for(int i=0;i<profileFieldsLayout.getChildCount();i++){
			View row=profileFieldsLayout.getChildAt(i);
			EditText title=row.findViewById(R.id.title);
			EditText content=row.findViewById(R.id.content);
			fieldTitles.add(title.getText().toString());
			fieldValues.add(content.getText().toString());
		}
		outState.putStringArrayList("fieldTitles", fieldTitles);
		outState.putStringArrayList("fieldValues", fieldValues);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode!=Activity.RESULT_OK)
			return;
		ImageView img;
		Uri uri=data.getData();
		int size;
		if(requestCode==AVATAR_RESULT){
			img=avaImage;
			avatarUri=uri;
			size=V.dp(100);
		}else{
			img=coverImage;
			coverUri=uri;
			size=V.dp(1000);
		}
		img.setForeground(null);
		ViewImageLoader.load(img, null, new UrlImageLoaderRequest(uri, size, size));
	}
}
