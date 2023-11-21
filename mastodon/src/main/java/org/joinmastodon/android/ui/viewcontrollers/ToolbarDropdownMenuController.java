package org.joinmastodon.android.ui.viewcontrollers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.OutlineProviders;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ToolbarDropdownMenuController{
	private final HostFragment fragment;
	private FrameLayout windowView;
	private FrameLayout menuContainer;
	private boolean dismissing;
	private List<DropdownSubmenuController> controllerStack=new ArrayList<>();
	private Animator currentTransition;

	public ToolbarDropdownMenuController(HostFragment fragment){
		this.fragment=fragment;
	}

	public void show(DropdownSubmenuController initialSubmenu){
		if(windowView!=null)
			return;

		menuContainer=new FrameLayout(fragment.getActivity());
		menuContainer.setBackgroundResource(R.drawable.bg_m3_surface2);
		menuContainer.setOutlineProvider(OutlineProviders.roundedRect(4));
		menuContainer.setClipToOutline(true);
		menuContainer.setElevation(V.dp(6));
		View menuView=initialSubmenu.getView();
		menuView.setVisibility(View.VISIBLE);
		menuContainer.addView(menuView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		windowView=new WindowView(fragment.getActivity());
		int pad=V.dp(16);
		windowView.setPadding(pad, fragment.getToolbar().getHeight(), pad, pad);
		windowView.setClipToPadding(false);
		windowView.addView(menuContainer, new FrameLayout.LayoutParams(V.dp(200), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START));

		WindowManager.LayoutParams wlp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
		wlp.format=PixelFormat.TRANSLUCENT;
		wlp.token=fragment.getActivity().getWindow().getDecorView().getWindowToken();
		wlp.width=wlp.height=ViewGroup.LayoutParams.MATCH_PARENT;
		wlp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
		wlp.setTitle(fragment.getActivity().getString(R.string.dropdown_menu));
		fragment.getActivity().getWindowManager().addView(windowView, wlp);

		menuContainer.setPivotX(V.dp(100));
		menuContainer.setPivotY(0);
		menuContainer.setScaleX(.8f);
		menuContainer.setScaleY(.8f);
		menuContainer.setAlpha(0f);
		menuContainer.animate()
				.scaleX(1f)
				.scaleY(1f)
				.alpha(1f)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
				.setDuration(150)
				.withLayer()
				.start();
		controllerStack.add(initialSubmenu);
	}

	public void dismiss(){
		if(windowView==null || dismissing)
			return;
		dismissing=true;
		fragment.onDropdownWillDismiss();
		menuContainer.animate()
				.scaleX(.8f)
				.scaleY(.8f)
				.alpha(0f)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
				.setDuration(150)
				.withLayer()
				.withEndAction(()->{
					controllerStack.clear();
					fragment.getActivity().getWindowManager().removeView(windowView);
					menuContainer.removeAllViews();
					dismissing=false;
					windowView=null;
					menuContainer=null;
					fragment.onDropdownDismissed();
				})
				.start();
	}

	public void pushSubmenuController(DropdownSubmenuController controller){
		View prevView=menuContainer.getChildAt(menuContainer.getChildCount()-1);
		View newView=controller.getView();
		newView.setVisibility(View.VISIBLE);
		menuContainer.addView(newView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		controllerStack.add(controller);
		animateTransition(prevView, newView, true);
	}

	public void popSubmenuController(){
		if(menuContainer.getChildCount()<=1)
			throw new IllegalStateException();
		DropdownSubmenuController controller=controllerStack.remove(controllerStack.size()-1);
		controller.onDismiss();
		View top=menuContainer.getChildAt(menuContainer.getChildCount()-1);
		View prev=menuContainer.getChildAt(menuContainer.getChildCount()-2);
		prev.setVisibility(View.VISIBLE);
		animateTransition(prev, top, false);
	}

	private void animateTransition(View bottomView, View topView, boolean adding){
		if(currentTransition!=null)
			currentTransition.cancel();
		int origBottom=menuContainer.getBottom();
		menuContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			private final Rect tmpRect=new Rect();

			@Override
			public boolean onPreDraw(){
				menuContainer.getViewTreeObserver().removeOnPreDrawListener(this);

				AnimatorSet set=new AnimatorSet();
				ObjectAnimator slideIn;
				set.playTogether(
						ObjectAnimator.ofInt(menuContainer, "bottom", origBottom, menuContainer.getTop()+(adding ? topView : bottomView).getHeight()),
						slideIn=ObjectAnimator.ofFloat(topView, View.TRANSLATION_X, adding ? menuContainer.getWidth() : 0, adding ? 0 : menuContainer.getWidth()),
						ObjectAnimator.ofFloat(bottomView, View.TRANSLATION_X, adding ? 0 : -menuContainer.getWidth()/4f, adding ? -menuContainer.getWidth()/4f : 0),
						ObjectAnimator.ofFloat(bottomView, View.ALPHA, adding ? 1f : 0f, adding ? 0f : 1f)
				);
				set.setDuration(300);
				set.setInterpolator(CubicBezierInterpolator.DEFAULT);
				set.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						bottomView.setClipBounds(null);
						bottomView.setTranslationX(0);
						bottomView.setAlpha(1f);
						topView.setTranslationX(0);
						topView.setAlpha(1f);
						if(adding){
							bottomView.setVisibility(View.GONE);
						}else{
							menuContainer.removeView(topView);
						}
						currentTransition=null;
					}
				});
				slideIn.addUpdateListener(animation->{
					tmpRect.set(0, 0, Math.round(topView.getX()-bottomView.getX()), bottomView.getHeight());
					bottomView.setClipBounds(tmpRect);
				});
				currentTransition=set;
				set.start();

				return true;
			}
		});
	}

	public void resizeOnNextFrame(){
		if(currentTransition!=null)
			currentTransition.cancel();
		if(windowView==null)
			return;
		int origBottom=menuContainer.getBottom();
		menuContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				menuContainer.getViewTreeObserver().removeOnPreDrawListener(this);

				ObjectAnimator anim=ObjectAnimator.ofInt(menuContainer, "bottom", origBottom, menuContainer.getBottom());
				anim.setDuration(300);
				anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
				anim.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						currentTransition=null;
					}
				});
				currentTransition=anim;
				anim.start();

				return true;
			}
		});
	}

	Activity getActivity(){
		return fragment.getActivity();
	}

	String getAccountID(){
		return fragment.getAccountID();
	}

	private class WindowView extends FrameLayout{
		private final Rect tmpRect=new Rect();
		public WindowView(@NonNull Context context){
			super(context);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev){
			for(int i=0;i<getChildCount();i++){
				View child=getChildAt(i);
				child.getHitRect(tmpRect);
				if(tmpRect.contains(Math.round(ev.getX()), Math.round(ev.getY())))
					return super.onTouchEvent(ev);
			}
			if(ev.getAction()==MotionEvent.ACTION_DOWN){
				dismiss();
			}
			return true;
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev){
			if(currentTransition!=null)
				return false;
			return super.dispatchTouchEvent(ev);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event){
			if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){
				if(event.getAction()==KeyEvent.ACTION_DOWN){
					if(controllerStack.size()>1)
						popSubmenuController();
					else
						dismiss();
				}
				return true;
			}
			return super.dispatchKeyEvent(event);
		}
	}

	public interface HostFragment{
		// Fragment methods
		Activity getActivity();
		Resources getResources();
		Toolbar getToolbar();
		String getAccountID();

		// Callbacks
		void onDropdownWillDismiss();
		void onDropdownDismissed();
	}
}
