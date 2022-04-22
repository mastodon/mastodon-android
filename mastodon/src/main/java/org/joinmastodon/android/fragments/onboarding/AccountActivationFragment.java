package org.joinmastodon.android.fragments.onboarding;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.Toast;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.ResendConfirmationEmail;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.SettingsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.util.Collections;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class AccountActivationFragment extends AppKitFragment{
	private String accountID;

	private Button btn, backBtn;
	private View buttonBar;
	private Handler uiHandler=new Handler(Looper.getMainLooper());
	private Runnable pollRunnable=this::tryGetAccount;
	private APIRequest currentRequest;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_activation, container, false);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		btn.setOnLongClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), SettingsFragment.class, args);
			return true;
		});
		buttonBar=view.findViewById(R.id.button_bar);
		view.findViewById(R.id.btn_back).setOnClickListener(v->onBackButtonClick());

		return view;
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
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

	private void onButtonClick(){
		try{
			startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}catch(ActivityNotFoundException x){
			Toast.makeText(getActivity(), R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	private void onBackButtonClick(){
		new ResendConfirmationEmail(null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						Toast.makeText(getActivity(), R.string.resent_email, Toast.LENGTH_SHORT).show();
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
		currentRequest=new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						currentRequest=null;
						AccountSessionManager mgr=AccountSessionManager.getInstance();
						AccountSession session=mgr.getAccount(accountID);
						mgr.removeAccount(accountID);
						mgr.addAccount(mgr.getInstanceInfo(session.domain), session.token, result, session.app, true);
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
}
