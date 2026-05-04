package org.joinmastodon.android.ui.tooltips;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.OutlineProviders;

import me.grishka.appkit.utils.V;

public abstract class BaseRichTooltip extends PopupWindow{
	protected final Context context;
	protected View contentView;

	public BaseRichTooltip(Context context){
		this.context=context;
		setOutsideTouchable(true);
	}

	protected abstract void initView();

	private void doInitView(){
		if(contentView!=null)
			return;
		initView();

		View contentWrap=contentView.findViewById(R.id.content_wrap);
		contentWrap.setOutlineProvider(OutlineProviders.roundedRect(12));
		contentWrap.setClipToOutline(true);

		setContentView(contentView);
	}

	public void show(View anchor){
		doInitView();
		contentView.measure(View.MeasureSpec.AT_MOST|V.dp(312), View.MeasureSpec.UNSPECIFIED);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		setHeight(contentView.getMeasuredHeight());
		showAsDropDown(anchor);
	}
}
