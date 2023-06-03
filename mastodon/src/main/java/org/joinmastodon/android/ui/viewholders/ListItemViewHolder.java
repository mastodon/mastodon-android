package org.joinmastodon.android.ui.viewholders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class ListItemViewHolder<T extends ListItem<?>> extends BindableViewHolder<T> implements UsableRecyclerView.DisableableClickable{
	protected final TextView title;
	protected final TextView subtitle;
	protected final ImageView icon;
	protected final LinearLayout view;

	public ListItemViewHolder(Context context, int layout, ViewGroup parent){
		super(context, layout, parent);
		title=findViewById(R.id.title);
		subtitle=findViewById(R.id.subtitle);
		icon=findViewById(R.id.icon);
		view=(LinearLayout) itemView;
	}

	@Override
	public void onBind(T item){
		if(TextUtils.isEmpty(item.title))
			title.setText(item.titleRes);
		else
			title.setText(item.title);

		if(TextUtils.isEmpty(item.subtitle) && item.subtitleRes==0){
			subtitle.setVisibility(View.GONE);
			title.setMaxLines(2);
			view.setMinimumHeight(V.dp(56));
		}else{
			subtitle.setVisibility(View.VISIBLE);
			title.setMaxLines(1);
			view.setMinimumHeight(V.dp(72));
			if(TextUtils.isEmpty(item.subtitle))
				subtitle.setText(item.subtitleRes);
			else
				subtitle.setText(item.subtitle);
		}

		if(item.iconRes!=0){
			icon.setVisibility(View.VISIBLE);
			icon.setImageResource(item.iconRes);
		}else{
			icon.setVisibility(View.GONE);
		}

		if(item.colorOverrideAttr!=0){
			int color=UiUtils.getThemeColor(view.getContext(), item.colorOverrideAttr);
			title.setTextColor(color);
			icon.setImageTintList(ColorStateList.valueOf(color));
		}

		view.setAlpha(item.isEnabled ? 1 : .4f);
	}

	@Override
	public boolean isEnabled(){
		return item.isEnabled;
	}

	@Override
	public void onClick(){
		item.onClick.run();
	}
}
