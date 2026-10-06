package org.joinmastodon.android.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

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
		if(toolbar!=null){
			if(toolbar.getNavigationIcon()!=null){
				toolbar.setNavigationContentDescription(R.string.back);
			}
			toolbarTitleView.addOnLayoutChangeListener(UiUtils::centerTextViewInToolbar);
			if(toolbarSubtitleView!=null){
				toolbarSubtitleView.addOnLayoutChangeListener(UiUtils::centerTextViewInToolbar);
			}
		}
	}
}
