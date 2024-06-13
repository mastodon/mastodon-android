package org.joinmastodon.android;

import android.os.Bundle;

import org.joinmastodon.android.fragments.DonationWebViewFragment;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

// This exists because our designer wanted to avoid extra sheet showing/hiding animations.
// This is the only way to show a fragment on top of a sheet without having to rewrite way too many things.
public class DonationFragmentActivity extends FragmentStackActivity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(savedInstanceState==null){
			DonationWebViewFragment fragment=new DonationWebViewFragment();
			fragment.setArguments(getIntent().getBundleExtra("fragmentArgs"));
			showFragment(fragment);
			overridePendingTransition(R.anim.fragment_enter, R.anim.no_op_300ms);
		}
	}

	@Override
	public void finish(){
		super.finish();
		overridePendingTransition(0, R.anim.fragment_exit);
	}
}
