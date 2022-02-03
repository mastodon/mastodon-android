package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;

import java.util.function.IntConsumer;

import androidx.annotation.IdRes;

public class TabBar extends LinearLayout{
	@IdRes
	private int selectedTabID;
	private IntConsumer listener;

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
			if(selectedTabID==0){
				selectedTabID=child.getId();
				child.setSelected(true);
			}
			child.setOnClickListener(this::onChildClick);
		}
	}

	private void onChildClick(View v){
		if(v.getId()==selectedTabID)
			return;
		findViewById(selectedTabID).setSelected(false);
		v.setSelected(true);
		selectedTabID=v.getId();
		listener.accept(selectedTabID);
	}

	public void setListener(IntConsumer listener){
		this.listener=listener;
	}
}
