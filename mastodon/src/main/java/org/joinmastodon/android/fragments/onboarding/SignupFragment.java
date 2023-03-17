package org.joinmastodon.android.fragments.onboarding;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joinmastodon.android.R;
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
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.parceler.Parcels;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class SignupFragment extends ToolbarFragment{
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
	private ElevationOnScrollListener onScrollListener;

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
		view.findViewById(R.id.scroller).setOnScrollChangeListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	private void onButtonClick(){
		if(!password.getText().toString().equals(passwordConfirm.getText().toString())){
			passwordConfirmWrap.setErrorState(getString(R.string.signup_passwords_dont_match));
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
								List<MastodonDetailedErrorResponse.FieldError> errors=Objects.requireNonNull(fieldErrors.get(fieldName));
								if(errors.size()==1){
									getFieldWrapByName(fieldName).setErrorState(getErrorDescription(errors.get(0), fieldName));
								}else{
									SpannableStringBuilder ssb=new SpannableStringBuilder();
									boolean firstErr=true;
									for(MastodonDetailedErrorResponse.FieldError err:errors){
										if(firstErr){
											firstErr=false;
										}else{
											ssb.append('\n');
										}
										ssb.append(getErrorDescription(err, fieldName));
									}
									getFieldWrapByName(fieldName).setErrorState(getErrorDescription(errors.get(0), fieldName));
								}
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

	private CharSequence getErrorDescription(MastodonDetailedErrorResponse.FieldError error, String fieldName){
		return switch(fieldName){
			case "email" -> switch(error.error){
				case "ERR_BLOCKED" -> {
					String emailAddr=email.getText().toString();
					String s=getResources().getString(R.string.signup_email_domain_blocked, TextUtils.htmlEncode(instance.uri), TextUtils.htmlEncode(emailAddr.substring(emailAddr.lastIndexOf('@')+1)));
					SpannableStringBuilder ssb=new SpannableStringBuilder();
					Jsoup.parseBodyFragment(s).body().traverse(new NodeVisitor(){
						private int spanStart;
						@Override
						public void head(Node node, int depth){
							if(node instanceof TextNode tn){
								ssb.append(tn.text());
							}else if(node instanceof Element){
								spanStart=ssb.length();
							}
						}

						@Override
						public void tail(Node node, int depth){
							if(node instanceof Element){
								ssb.setSpan(new LinkSpan("", SignupFragment.this::onGoBackLinkClick, LinkSpan.Type.CUSTOM, null), spanStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
								ssb.setSpan(new TypefaceSpan("sans-serif-medium"), spanStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
						}
					});
					yield ssb;
				}
				default -> error.description;
			};
			default -> error.description;
		};
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

	private void onGoBackLinkClick(LinkSpan span){
		setResult(false, null);
		Nav.finish(this);
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
