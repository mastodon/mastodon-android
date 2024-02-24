package org.joinmastodon.android.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.ViewProperties;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import androidx.annotation.IdRes;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class TabBar extends LinearLayout{
	@IdRes
	private int selectedTabID;
	private IntConsumer listener;
	private IntPredicate longClickListener;
	private Typeface mediumFont=Typeface.create("sans-serif-medium", Typeface.NORMAL), boldFont=Typeface.DEFAULT_BOLD;

	public TabBar(Context context){
		this(context, null);
	}

	public TabBar(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public TabBar(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	public void onViewAdded(View child){
		super.onViewAdded(child);
		if(child.getId()!=0){
			ViewHolder holder=new ViewHolder();
			holder.label=child.findViewById(R.id.label);
			child.setTag(holder);
			if(selectedTabID==0){
				selectedTabID=child.getId();
				setTabSelected(child, true);
			}else{
				holder.label.setTypeface(mediumFont);
			}
			child.setOnClickListener(this::onChildClick);
			child.setOnLongClickListener(this::onChildLongClick);
		}
	}

	private void onChildClick(View v){
		listener.accept(v.getId());
		if(v.getId()==selectedTabID)
			return;
		setTabSelected(findViewById(selectedTabID), false);
		setTabSelected(v, true);
		selectedTabID=v.getId();
	}

	private boolean onChildLongClick(View v){
		return longClickListener.test(v.getId());
	}

	public void setListeners(IntConsumer listener, IntPredicate longClickListener){
		this.listener=listener;
		this.longClickListener=longClickListener;
	}

	public void selectTab(int id){
		setTabSelected(findViewById(selectedTabID), false);
		selectedTabID=id;
		setTabSelected(findViewById(selectedTabID), true);
	}

	private void setTabSelected(View tab, boolean selected){
		tab.setSelected(selected);
		ViewHolder holder=(ViewHolder) tab.getTag();
		if(holder.currentAnim!=null)
			holder.currentAnim.cancel();
		ArrayList<Animator> anims=new ArrayList<>();
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
			anims.add(ObjectAnimator.ofInt(holder.label, ViewProperties.FONT_WEIGHT, selected ? 700 : 500));
		}else{
			holder.label.setTypeface(selected ? boldFont : mediumFont);
		}
		anims.add(ObjectAnimator.ofArgb(holder.label, ViewProperties.TEXT_COLOR, UiUtils.getThemeColor(getContext(), selected ? R.attr.colorM3OnSurface : R.attr.colorM3OnSurfaceVariant)));
		AnimatorSet set=new AnimatorSet();
		set.playTogether(anims);
		set.setDuration(400);
//		set.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.m3_sys_motion_easing_standard));
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		holder.currentAnim=set;
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				holder.currentAnim=null;
			}
		});
		set.start();
	}

	private static class ViewHolder{
		public TextView label;
		public Animator currentAnim;
	}
}
