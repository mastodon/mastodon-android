package org.joinmastodon.android;

import android.app.Application;
import android.app.Fragment;
import android.os.Bundle;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

public class MainActivity extends FragmentStackActivity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		if(savedInstanceState==null){
			if(AccountSessionManager.getInstance().getLoggedInAccounts().isEmpty()){
				showFragmentClearingBackStack(new SplashFragment());
			}else{
				AccountSessionManager.getInstance().maybeUpdateLocalInfo();
				AccountSession session=AccountSessionManager.getInstance().getLastActiveAccount();
				Bundle args=new Bundle();
				args.putString("account", session.getID());
				Fragment fragment=session.activated ? new HomeFragment() : new AccountActivationFragment();
				fragment.setArguments(args);
				showFragmentClearingBackStack(fragment);
			}
		}

		if(BuildConfig.BUILD_TYPE.startsWith("appcenter")){
			// Call the appcenter SDK wrapper through reflection because it is only present in beta builds
			try{
				Class.forName("org.joinmastodon.android.AppCenterWrapper").getMethod("init", Application.class).invoke(null, getApplication());
			}catch(ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException ignore){}
		}
	}
}
