package org.joinmastodon.android.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import org.joinmastodon.android.R;

import androidx.annotation.CallSuper;
import me.grishka.appkit.fragments.ToolbarFragment;

public abstract class MastodonToolbarFragment extends ToolbarFragment{

	public MastodonToolbarFragment(){
		super();
	}

	protected MastodonToolbarFragment(int layout){
		super(layout);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@CallSuper
	protected void updateToolbar(){
		Toolbar toolbar=getToolbar();
		if(toolbar!=null && toolbar.getNavigationIcon()!=null){
			toolbar.setNavigationContentDescription(R.string.back);
		}
	}
}
