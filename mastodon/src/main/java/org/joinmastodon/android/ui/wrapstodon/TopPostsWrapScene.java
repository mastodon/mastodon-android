package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.view.View;

import org.joinmastodon.android.model.Status;

public class TopPostsWrapScene extends AnnualWrapScene{
	private Status mostBoosted, mostFavorited, mostReplied;

	@Override
	protected View onCreateContentView(Context context){
		return null;
	}

	@Override
	protected void onDestroyContentView(){

	}
}
