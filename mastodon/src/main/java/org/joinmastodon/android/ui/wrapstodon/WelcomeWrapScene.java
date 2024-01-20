package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;

public class WelcomeWrapScene extends AnnualWrapScene{
	@Override
	protected View onCreateContentView(Context context){
		View view=context.getSystemService(LayoutInflater.class).inflate(R.layout.wrap_welcome, null);
		TextView title=view.findViewById(R.id.title);
		title.setText(context.getString(R.string.yearly_wrap_intro_title, year));
		return view;
	}

	@Override
	protected void onDestroyContentView(){

	}
}
