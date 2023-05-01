package org.joinmastodon.android.ui;

import android.graphics.Outline;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewOutlineProvider;

import me.grishka.appkit.utils.V;

public class OutlineProviders{
	private static final SparseArray<ViewOutlineProvider> roundedRects=new SparseArray<>();
	private static final SparseArray<ViewOutlineProvider> topRoundedRects=new SparseArray<>();
	private static final SparseArray<ViewOutlineProvider> endRoundedRects=new SparseArray<>();

	public static final int RADIUS_XSMALL=4;
	public static final int RADIUS_SMALL=8;
	public static final int RADIUS_MEDIUM=12;
	public static final int RADIUS_LARGE=16;
	public static final int RADIUS_XLARGE=28;

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

	public static ViewOutlineProvider topRoundedRect(int dp){
		ViewOutlineProvider provider=topRoundedRects.get(dp);
		if(provider!=null)
			return provider;
		provider=new TopRoundRectOutlineProvider(V.dp(dp));
		topRoundedRects.put(dp, provider);
		return provider;
	}

	public static ViewOutlineProvider endRoundedRect(int dp){
		ViewOutlineProvider provider=endRoundedRects.get(dp);
		if(provider!=null)
			return provider;
		provider=new EndRoundRectOutlineProvider(V.dp(dp));
		endRoundedRects.put(dp, provider);
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

	private static class TopRoundRectOutlineProvider extends ViewOutlineProvider{
		private final int radius;

		private TopRoundRectOutlineProvider(int radius){
			this.radius=radius;
		}

		@Override
		public void getOutline(View view, Outline outline){
			outline.setRoundRect(0, 0, view.getWidth(), view.getHeight()+radius, radius);
		}
	}

	private static class EndRoundRectOutlineProvider extends ViewOutlineProvider{
		private final int radius;

		private EndRoundRectOutlineProvider(int radius){
			this.radius=radius;
		}

		@Override
		public void getOutline(View view, Outline outline){
			if(view.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL){
				outline.setRoundRect(-radius, 0, view.getWidth(), view.getHeight(), radius);
			}else{
				outline.setRoundRect(0, 0, view.getWidth()+radius, view.getHeight(), radius);
			}
		}
	}
}
