package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.onboarding.InstanceCatalogFragment;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class SplashFragment extends AppKitFragment{

	private View contentView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentView= inflater.inflate(R.layout.fragment_splash, container, false);
		contentView.findViewById(R.id.btn_get_started).setOnClickListener(this::onButtonClick);
		contentView.findViewById(R.id.btn_log_in).setOnClickListener(this::onButtonClick);
		return contentView;
	}

	private void onButtonClick(View v){
		Bundle extras=new Bundle();
		extras.putBoolean("signup", v.getId()==R.id.btn_get_started);
		Nav.go(getActivity(), InstanceCatalogFragment.class, extras);
	}
//
//	@Override
//	public void onApplyWindowInsets(WindowInsets insets){
//		if(contentView!=null)
//			contentView.dispatchApplyWindowInsets(insets);
//	}
}
