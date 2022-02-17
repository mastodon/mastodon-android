package org.joinmastodon.android.ui;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

public class OutlineProviders{
	private OutlineProviders(){
		//no instance
	}

	public static final ViewOutlineProvider BACKGROUND_WITH_ALPHA=new ViewOutlineProvider(){
		@Override
		public void getOutline(View view, Outline outline){
			view.getBackground().getOutline(outline);
			outline.setAlpha(view.getAlpha());
		}
	};
}
