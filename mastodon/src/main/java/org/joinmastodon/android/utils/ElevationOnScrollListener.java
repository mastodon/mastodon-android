package org.joinmastodon.android.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class ElevationOnScrollListener extends RecyclerView.OnScrollListener implements View.OnScrollChangeListener{
	private boolean isAtTop;
	private Animator currentPanelsAnim;
	private List<View> views;
	private FragmentRootLinearLayout fragmentRootLayout;
	private Rect tmpRect=new Rect();

	public ElevationOnScrollListener(FragmentRootLinearLayout fragmentRootLayout, View... views){
		this(fragmentRootLayout, Arrays.asList(views));
	}

	public ElevationOnScrollListener(FragmentRootLinearLayout fragmentRootLayout, List<View> views){
		isAtTop=true;
		this.fragmentRootLayout=fragmentRootLayout;
		this.views=views;
		for(View v:views){
			Drawable bg=v.getContext().getDrawable(R.drawable.bg_onboarding_panel).mutate();
			v.setBackground(bg);
			if(bg instanceof LayerDrawable ld){
				Drawable overlay=ld.findDrawableByLayerId(R.id.color_overlay);
				if(overlay!=null){
					overlay.setAlpha(0);
				}
			}
		}
	}

	public void setViews(View... views){
		setViews(Arrays.asList(views));
	}

	public void setViews(List<View> views){
		List<View> oldViews=this.views;
		this.views=views;
		for(View v:views){
			if(oldViews.contains(v))
				continue;
			Drawable bg=v.getContext().getDrawable(R.drawable.bg_onboarding_panel).mutate();
			v.setBackground(bg);
			if(bg instanceof LayerDrawable ld){
				Drawable overlay=ld.findDrawableByLayerId(R.id.color_overlay);
				if(overlay!=null){
					overlay.setAlpha(isAtTop ? 0 : 20);
				}
			}
			v.setTranslationZ(isAtTop ? 0 : V.dp(3));
		}
	}

	private int getRecyclerChildDecoratedTop(RecyclerView rv, View child){
		rv.getDecoratedBoundsWithMargins(child, tmpRect);
		return tmpRect.top;
	}

	@Override
	public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
		boolean newAtTop=recyclerView.getChildCount()==0 || (recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0))==0 && getRecyclerChildDecoratedTop(recyclerView, recyclerView.getChildAt(0))==recyclerView.getPaddingTop());
		handleScroll(recyclerView.getContext(), newAtTop);
	}

	@Override
	public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		handleScroll(v.getContext(), scrollY<=0);
	}

	private void handleScroll(Context context, boolean newAtTop){
		if(newAtTop!=isAtTop){
			isAtTop=newAtTop;
			if(currentPanelsAnim!=null)
				currentPanelsAnim.cancel();

			AnimatorSet set=new AnimatorSet();
			ArrayList<Animator> anims=new ArrayList<>();
			for(View v:views){
				if(v.getBackground() instanceof LayerDrawable ld){
					Drawable overlay=ld.findDrawableByLayerId(R.id.color_overlay);
					if(overlay!=null){
						anims.add(ObjectAnimator.ofInt(overlay, "alpha", isAtTop ? 0 : 20));
					}
				}
				anims.add(ObjectAnimator.ofFloat(v, View.TRANSLATION_Z, isAtTop ? 0 : V.dp(3)));
			}
			if(fragmentRootLayout!=null){
				int color;
				if(isAtTop){
					color=UiUtils.getThemeColor(context, R.attr.colorM3Background);
				}else{
					color=UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Background), UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.07843137f);
				}
				anims.add(ObjectAnimator.ofArgb(fragmentRootLayout, "statusBarColor", color));
			}
			set.playTogether(anims);
			set.setDuration(150);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					currentPanelsAnim=null;
				}
			});
			set.start();
			currentPanelsAnim=set;
		}
	}

	public int getCurrentStatusBarColor(){
		return fragmentRootLayout.getStatusBarColor();
	}
}
