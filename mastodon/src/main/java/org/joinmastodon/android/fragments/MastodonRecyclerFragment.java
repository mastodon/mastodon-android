package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.Collections;
import java.util.List;

import androidx.annotation.CallSuper;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public abstract class MastodonRecyclerFragment<T> extends BaseRecyclerFragment<T>{
	protected ElevationOnScrollListener elevationOnScrollListener;
	protected boolean doReauthOnErrorRetry;

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
		if(wantsElevationOnScrollEffect()){
			FragmentRootLinearLayout rootView;
			if(view instanceof FragmentRootLinearLayout frl)
				rootView=frl;
			else
				rootView=view.findViewById(R.id.appkit_loader_root);
			list.addOnScrollListener(elevationOnScrollListener=new ElevationOnScrollListener(rootView, getViewsForElevationEffect()));
		}
		list.setItemAnimator(new BetterItemAnimator());
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

	@Override
	public void onError(ErrorResponse error){
		if(!refreshing && data.isEmpty() && error instanceof MastodonErrorResponse mer && mer.httpStatus==401){
			doReauthOnErrorRetry=true;
		}
		super.onError(error);
	}

	@Override
	protected void onErrorRetryClick(){
		if(doReauthOnErrorRetry){
			doReauthOnErrorRetry=false;
			String accountID=getArguments().getString("account");
			AccountSession session=AccountSessionManager.get(accountID);
			AccountSessionManager.getInstance().authenticate(getActivity(), session.getInstanceInfo(), session.app);
			return;
		}
		super.onErrorRetryClick();
	}
}
