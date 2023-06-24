package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.Collections;
import java.util.List;

import androidx.annotation.CallSuper;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public abstract class MastodonRecyclerFragment<T> extends BaseRecyclerFragment<T>{
	protected ElevationOnScrollListener elevationOnScrollListener;

	public MastodonRecyclerFragment(int perPage){
		super(perPage);
	}

	public MastodonRecyclerFragment(int layout, int perPage){
		super(layout, perPage);
	}

	protected List<View> getViewsForElevationEffect(){
		Toolbar toolbar=getToolbar();
		return toolbar!=null ? Collections.singletonList(toolbar) : Collections.emptyList();
	}

	@Override
	@CallSuper
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(wantsElevationOnScrollEffect())
			list.addOnScrollListener(elevationOnScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, getViewsForElevationEffect()));
		if(refreshLayout!=null){
			int colorBackground=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background);
			int colorPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
			refreshLayout.setProgressBackgroundColorSchemeColor(UiUtils.alphaBlendColors(colorBackground, colorPrimary, 0.11f));
			refreshLayout.setColorSchemeColors(colorPrimary);
		}
	}

	@Override
	@CallSuper
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(elevationOnScrollListener!=null){
			elevationOnScrollListener.setViews(getViewsForElevationEffect());
		}
	}

	protected boolean wantsElevationOnScrollEffect(){
		return true;
	}
}
