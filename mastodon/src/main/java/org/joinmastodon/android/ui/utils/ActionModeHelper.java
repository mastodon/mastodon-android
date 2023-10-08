package org.joinmastodon.android.ui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;

import java.util.function.IntSupplier;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.fragments.AppKitFragment;

public class ActionModeHelper{
	public static ActionMode startActionMode(AppKitFragment fragment, IntSupplier statusBarColorSupplier, ActionMode.Callback callback){
		FragmentStackActivity activity=(FragmentStackActivity) fragment.getActivity();
		return activity.startActionMode(new ActionMode.Callback(){
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu){
				if(!callback.onCreateActionMode(mode, menu))
					return false;
				ObjectAnimator anim=ObjectAnimator.ofInt(activity.getWindow(), "statusBarColor", statusBarColorSupplier.getAsInt(), UiUtils.getThemeColor(activity, R.attr.colorM3Primary));
				anim.setEvaluator(new IntEvaluator(){
					@Override
					public Integer evaluate(float fraction, Integer startValue, Integer endValue){
						return UiUtils.alphaBlendColors(startValue, endValue, fraction);
					}
				});
				anim.start();
				activity.invalidateSystemBarColors(fragment);
				View fakeView=new View(activity);
//				mode.setCustomView(fakeView);
//				int buttonID=activity.getResources().getIdentifier("action_mode_close_button", "id", "android");
//				View btn=activity.getWindow().getDecorView().findViewById(buttonID);
//				if(btn!=null){
//					((ViewGroup.MarginLayoutParams)btn.getLayoutParams()).setMarginEnd(0);
//				}
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu){
				if(!callback.onPrepareActionMode(mode, menu))
					return false;
				for(int i=0;i<menu.size();i++){
					Drawable icon=menu.getItem(i).getIcon();
					if(icon!=null){
						icon=icon.mutate();
						icon.setTint(UiUtils.getThemeColor(activity, R.attr.colorM3OnPrimary));
						menu.getItem(i).setIcon(icon);
					}
				}
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item){
				return callback.onActionItemClicked(mode, item);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode){
				ObjectAnimator anim=ObjectAnimator.ofInt(activity.getWindow(), "statusBarColor", UiUtils.getThemeColor(activity, R.attr.colorM3Primary), statusBarColorSupplier.getAsInt());
				anim.setEvaluator(new IntEvaluator(){
					@Override
					public Integer evaluate(float fraction, Integer startValue, Integer endValue){
						return UiUtils.alphaBlendColors(startValue, endValue, fraction);
					}
				});
				anim.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						activity.getWindow().setStatusBarColor(0);
					}
				});
				anim.start();
				activity.invalidateSystemBarColors(fragment);
				callback.onDestroyActionMode(mode);
			}
		});
	}
}
