package org.joinmastodon.android.ui;

import android.graphics.Outline;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewOutlineProvider;

import me.grishka.appkit.utils.V;

public class OutlineProviders{
	private static SparseArray<ViewOutlineProvider> roundedRects=new SparseArray<>();

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
	public static final ViewOutlineProvider OVAL=new ViewOutlineProvider(){
		@Override
		public void getOutline(View view, Outline outline){
			outline.setOval(0, 0, view.getWidth(), view.getHeight());
		}
	};

	public static ViewOutlineProvider roundedRect(int dp){
		ViewOutlineProvider provider=roundedRects.get(dp);
		if(provider!=null)
			return provider;
		provider=new RoundRectOutlineProvider(V.dp(dp));
		roundedRects.put(dp, provider);
		return provider;
	}

	private static class RoundRectOutlineProvider extends ViewOutlineProvider{
		private final int radius;

		private RoundRectOutlineProvider(int radius){
			this.radius=radius;
		}

		@Override
		public void getOutline(View view, Outline outline){
			outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
		}
	}
}
