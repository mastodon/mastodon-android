package org.joinmastodon.android.fragments.onboarding;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.ResendConfirmationEmail;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.SettingsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.util.Collections;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.V;

public class AccountActivationFragment extends ToolbarFragment{
	private String accountID;

	private Button openEmailBtn, resendBtn;
	private View contentView;
	private Handler uiHandler=new Handler(Looper.getMainLooper());
	private Runnable pollRunnable=this::tryGetAccount;
	private APIRequest currentRequest;
	private Runnable resendTimer=this::updateResendTimer;
	private long lastResendTime;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.confirm_email_title);
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		lastResendTime=session.activationInfo!=null ? session.activationInfo.lastEmailConfirmationResend : 0;
	}

	@Nullable
	@Override
	public View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_activation, container, false);

		openEmailBtn=view.findViewById(R.id.btn_next);
		openEmailBtn.setOnClickListener(this::onOpenEmailClick);
		openEmailBtn.setOnLongClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), SettingsFragment.class, args);
			return true;
		});
		resendBtn=view.findViewById(R.id.btn_resend);
		resendBtn.setOnClickListener(this::onResendClick);
		TextView text=view.findViewById(R.id.subtitle);
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		text.setText(getString(R.string.confirm_email_subtitle, session.activationInfo!=null ? session.activationInfo.email : "?"));
		updateResendTimer();

		contentView=view;
		return view;
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
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

	@Override
	protected boolean canGoBack(){
		return true;
	}

	@Override
	public void onToolbarNavigationClick(){
		new AccountSwitcherSheet(getActivity()).show();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			contentView.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
		tryGetAccount();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}else{
			uiHandler.removeCallbacks(pollRunnable);
		}
	}

	private void onOpenEmailClick(View v){
		try{
			startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}catch(ActivityNotFoundException x){
			Toast.makeText(getActivity(), R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	private void onResendClick(View v){
		new ResendConfirmationEmail(null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						Toast.makeText(getActivity(), R.string.resent_email, Toast.LENGTH_SHORT).show();
						AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
						if(session.activationInfo==null){
							session.activationInfo=new AccountActivationInfo("?", System.currentTimeMillis());
						}else{
							session.activationInfo.lastEmailConfirmationResend=System.currentTimeMillis();
						}
						lastResendTime=session.activationInfo.lastEmailConfirmationResend;
						AccountSessionManager.getInstance().writeAccountsFile();
						updateResendTimer();
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	private void tryGetAccount(){
		if(AccountSessionManager.getInstance().tryGetAccount(accountID)==null){
			uiHandler.removeCallbacks(pollRunnable);
			getActivity().finish();
			Intent intent=new Intent(getActivity(), MainActivity.class);
			startActivity(intent);
			return;
		}
		currentRequest=new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						currentRequest=null;
						AccountSessionManager mgr=AccountSessionManager.getInstance();
						AccountSession session=mgr.getAccount(accountID);
						mgr.removeAccount(accountID);
						mgr.addAccount(mgr.getInstanceInfo(session.domain), session.token, result, session.app, null);
						String newID=mgr.getLastActiveAccountID();
						Bundle args=new Bundle();
						args.putString("account", newID);
						if(session.self.avatar!=null || session.self.displayName!=null){
							File avaFile=session.self.avatar!=null ? new File(session.self.avatar) : null;
							new UpdateAccountCredentials(session.self.displayName, "", avaFile, null, Collections.emptyList())
									.setCallback(new Callback<>(){
										@Override
										public void onSuccess(Account result){
											if(avaFile!=null)
												avaFile.delete();
											mgr.updateAccountInfo(newID, result);
											Nav.goClearingStack(getActivity(), HomeFragment.class, args);
										}

										@Override
										public void onError(ErrorResponse error){
											if(avaFile!=null)
												avaFile.delete();
											Nav.goClearingStack(getActivity(), HomeFragment.class, args);
										}
									})
									.exec(newID);
						}else{
							Nav.goClearingStack(getActivity(), HomeFragment.class, args);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						uiHandler.postDelayed(pollRunnable, 10_000L);
					}
				})
				.exec(accountID);
	}

	@SuppressLint("DefaultLocale")
	private void updateResendTimer(){
		long sinceResend=System.currentTimeMillis()-lastResendTime;
		if(sinceResend>59_000L){
			resendBtn.setText(R.string.resend);
			resendBtn.setEnabled(true);
			return;
		}
		int seconds=(int)((60_000L-sinceResend)/1000L);
		resendBtn.setText(String.format("%s (%d)", getString(R.string.resend), seconds));
		if(resendBtn.isEnabled())
			resendBtn.setEnabled(false);
		resendBtn.postDelayed(resendTimer, 500);
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		resendBtn.removeCallbacks(resendTimer);
	}
}
