package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;

import androidx.annotation.Nullable;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment{
	private FragmentRootLinearLayout content;
	private HomeTimelineFragment homeTimelineFragment;

	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		FrameLayout fragmentContainer=new FrameLayout(getActivity());
		fragmentContainer.setId(R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		Bundle args=new Bundle();
		args.putString("account", accountID);
		homeTimelineFragment=new HomeTimelineFragment();
		homeTimelineFragment.setArguments(args);
		getChildFragmentManager().beginTransaction().add(R.id.fragment_wrap, homeTimelineFragment).commit();

		return content;
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		homeTimelineFragment.onHiddenChanged(hidden);
	}
}
