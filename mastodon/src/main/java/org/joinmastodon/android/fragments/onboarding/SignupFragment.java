package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.MastodonDetailedErrorResponse;
import org.joinmastodon.android.api.requests.accounts.RegisterAccount;
import org.joinmastodon.android.api.requests.oauth.CreateOAuthApp;
import org.joinmastodon.android.api.requests.oauth.GetOauthToken;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Token;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class SignupFragment extends ToolbarFragment{
	private static final int AVATAR_RESULT=198;
	private static final String TAG="SignupFragment";

	private Instance instance;

	private EditText displayName, username, email, password, passwordConfirm, reason;
	private FloatingHintEditTextLayout displayNameWrap, usernameWrap, emailWrap, passwordWrap, passwordConfirmWrap, reasonWrap;
	private TextView reasonExplain;
	private Button btn;
	private View buttonBar;
	private TextWatcher buttonStateUpdater=new SimpleTextWatcher(e->updateButtonState());
	private APIRequest currentBackgroundRequest;
	private Application apiApplication;
	private Token apiToken;
	private boolean submitAfterGettingToken;
	private ProgressDialog progressDialog;
	private HashSet<EditText> errorFields=new HashSet<>();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		instance=Parcels.unwrap(getArguments().getParcelable("instance"));
		createAppAndGetToken();
		setTitle(R.string.signup_title);
	}

	@Nullable
	@Override
	public View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_signup, container, false);

		TextView domain=view.findViewById(R.id.domain);
		displayName=view.findViewById(R.id.display_name);
		username=view.findViewById(R.id.username);
		email=view.findViewById(R.id.email);
		password=view.findViewById(R.id.password);
		passwordConfirm=view.findViewById(R.id.password_confirm);
		reason=view.findViewById(R.id.reason);
		reasonExplain=view.findViewById(R.id.reason_explain);

		displayNameWrap=view.findViewById(R.id.display_name_wrap);
		usernameWrap=view.findViewById(R.id.username_wrap);
		emailWrap=view.findViewById(R.id.email_wrap);
		passwordWrap=view.findViewById(R.id.password_wrap);
		passwordConfirmWrap=view.findViewById(R.id.password_confirm_wrap);
		reasonWrap=view.findViewById(R.id.reason_wrap);

		domain.setText('@'+instance.uri);

		username.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				username.getViewTreeObserver().removeOnPreDrawListener(this);
				username.setPadding(username.getPaddingLeft(), username.getPaddingTop(), domain.getWidth(), username.getPaddingBottom());
				return true;
			}
		});

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);
		updateButtonState();

		username.addTextChangedListener(buttonStateUpdater);
		email.addTextChangedListener(buttonStateUpdater);
		password.addTextChangedListener(buttonStateUpdater);
		passwordConfirm.addTextChangedListener(buttonStateUpdater);
		reason.addTextChangedListener(buttonStateUpdater);

		username.addTextChangedListener(new ErrorClearingListener(username));
		email.addTextChangedListener(new ErrorClearingListener(email));
		password.addTextChangedListener(new ErrorClearingListener(password));
		passwordConfirm.addTextChangedListener(new ErrorClearingListener(passwordConfirm));
		reason.addTextChangedListener(new ErrorClearingListener(reason));

		if(!instance.approvalRequired){
			reason.setVisibility(View.GONE);
			reasonExplain.setVisibility(View.GONE);
		}

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setBackground(null);
		getToolbar().setElevation(0);
	}

	private void onButtonClick(){
		if(!password.getText().toString().equals(passwordConfirm.getText().toString())){
			passwordConfirm.setError(getString(R.string.signup_passwords_dont_match));
			passwordConfirmWrap.setErrorState();
			return;
		}
		showProgressDialog();
		if(currentBackgroundRequest!=null){
			submitAfterGettingToken=true;
		}else if(apiApplication==null){
			submitAfterGettingToken=true;
			createAppAndGetToken();
		}else if(apiToken==null){
			submitAfterGettingToken=true;
			getToken();
		}else{
			submit();
		}
	}

	private void submit(){
		actuallySubmit();
	}

	private void actuallySubmit(){
		String username=this.username.getText().toString().trim();
		String email=this.email.getText().toString().trim();
		for(EditText edit:errorFields){
			edit.setError(null);
		}
		errorFields.clear();
		new RegisterAccount(username, email, password.getText().toString(), getResources().getConfiguration().locale.getLanguage(), reason.getText().toString())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Token result){
						progressDialog.dismiss();
						Account fakeAccount=new Account();
						fakeAccount.acct=fakeAccount.username=username;
						fakeAccount.id="tmp"+System.currentTimeMillis();
						fakeAccount.displayName=displayName.getText().toString();
						AccountSessionManager.getInstance().addAccount(instance, result, fakeAccount, apiApplication, new AccountActivationInfo(email, System.currentTimeMillis()));
						Bundle args=new Bundle();
						args.putString("account", AccountSessionManager.getInstance().getLastActiveAccountID());
						Nav.goClearingStack(getActivity(), AccountActivationFragment.class, args);
					}

					@Override
					public void onError(ErrorResponse error){
						if(error instanceof MastodonDetailedErrorResponse derr){
							Map<String, List<MastodonDetailedErrorResponse.FieldError>> fieldErrors=derr.detailedErrors;
							boolean first=true;
							boolean anyFieldsSkipped=false;
							for(String fieldName:fieldErrors.keySet()){
								EditText field=getFieldByName(fieldName);
								if(field==null){
									anyFieldsSkipped=true;
									continue;
								}
								field.setError(fieldErrors.get(fieldName).stream().map(err->err.description).collect(Collectors.joining("\n")));
								getFieldWrapByName(fieldName).setErrorState();
								errorFields.add(field);
								if(first){
									first=false;
									field.requestFocus();
								}
							}
							if(anyFieldsSkipped)
								error.showToast(getActivity());
						}else{
							error.showToast(getActivity());
						}
						progressDialog.dismiss();
					}
				})
				.exec(instance.uri, apiToken);
	}

	private EditText getFieldByName(String name){
		return switch(name){
			case "email" -> email;
			case "username" -> username;
			case "password" -> password;
			case "reason" -> reason;
			default -> null;
		};
	}

	private FloatingHintEditTextLayout getFieldWrapByName(String name){
		return switch(name){
			case "email" -> emailWrap;
			case "username" -> usernameWrap;
			case "password" -> passwordWrap;
			case "reason" -> reasonWrap;
			default -> null;
		};
	}

	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog=new ProgressDialog(getActivity());
			progressDialog.setMessage(getString(R.string.loading));
			progressDialog.setCancelable(false);
		}
		progressDialog.show();
	}

	private void updateButtonState(){
		btn.setEnabled(username.length()>0 && email.length()>0 && email.getText().toString().contains("@") && password.length()>=8 && passwordConfirm.length()>=8 && (!instance.approvalRequired || reason.length()>0));
	}

	private void createAppAndGetToken(){
		currentBackgroundRequest=new CreateOAuthApp()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Application result){
						apiApplication=result;
						getToken();
					}

					@Override
					public void onError(ErrorResponse error){
						currentBackgroundRequest=null;
						if(submitAfterGettingToken){
							submitAfterGettingToken=false;
							progressDialog.dismiss();
							error.showToast(getActivity());
						}
					}
				})
				.execNoAuth(instance.uri);
	}

	private void getToken(){
		currentBackgroundRequest=new GetOauthToken(apiApplication.clientId, apiApplication.clientSecret, null, GetOauthToken.GrantType.CLIENT_CREDENTIALS)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Token result){
						currentBackgroundRequest=null;
						apiToken=result;
						if(submitAfterGettingToken){
							submitAfterGettingToken=false;
							submit();
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentBackgroundRequest=null;
						if(submitAfterGettingToken){
							submitAfterGettingToken=false;
							progressDialog.dismiss();
							error.showToast(getActivity());
						}
					}
				})
				.execNoAuth(instance.uri);
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

	private class ErrorClearingListener implements TextWatcher{
		public final EditText editText;

		private ErrorClearingListener(EditText editText){
			this.editText=editText;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after){

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count){

		}

		@Override
		public void afterTextChanged(Editable s){
			if(errorFields.contains(editText)){
				errorFields.remove(editText);
				editText.setError(null);
			}
		}
	}
}
