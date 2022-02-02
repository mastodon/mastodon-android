package org.joinmastodon.android;

import android.os.Bundle;

import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.SplashFragment;

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
				Bundle args=new Bundle();
				args.putString("account", AccountSessionManager.getInstance().getLastActiveAccountID());
				HomeFragment fragment=new HomeFragment();
				fragment.setArguments(args);
				showFragmentClearingBackStack(fragment);
			}
		}
	}
}
