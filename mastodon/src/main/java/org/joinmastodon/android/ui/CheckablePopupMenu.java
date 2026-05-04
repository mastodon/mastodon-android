package org.joinmastodon.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableTextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.StringRes;
import me.grishka.appkit.utils.V;

public class CheckablePopupMenu extends PopupWindow{
	private final Context context;
	private List<Item> items=new ArrayList<>();
	private ItemCheckedChangedListener listener;

	private View contentView;

	public CheckablePopupMenu(Context context){
		this.context=context;
		setOutsideTouchable(true);
	}

	public CheckablePopupMenu addItem(CharSequence text, boolean checked){
		items.add(new Item(text, checked));
		return this;
	}

	public CheckablePopupMenu addItem(@StringRes int text, boolean checked){
		return addItem(context.getString(text), checked);
	}

	public CheckablePopupMenu setListener(ItemCheckedChangedListener listener){
		this.listener=listener;
		return this;
	}

	public View initView(){
		if(contentView!=null)
			return contentView;

		contentView=LayoutInflater.from(context).inflate(R.layout.checkable_popup_menu, null);

		LinearLayout contentWrap=contentView.findViewById(R.id.content_wrap);
		contentWrap.setOutlineProvider(OutlineProviders.roundedRect(12));
		contentWrap.setClipToOutline(true);
		for(int i=0;i<items.size();i++){
			Item item=items.get(i);
			CheckableTextView itemView=new CheckableTextView(context);
			itemView.setTextAppearance(R.style.m3_label_large);
			itemView.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3OnSurface));
			itemView.setText(item.title);
			itemView.setChecked(item.checked);
			itemView.setBackground(UiUtils.getThemeDrawable(context, android.R.attr.selectableItemBackground));
			itemView.setMinHeight(V.dp(48));
			itemView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
			itemView.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));
			itemView.setCompoundDrawablePadding(V.dp(8));
			itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_20px_checkable, 0);
			itemView.setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(context, R.attr.colorM3OnSurfaceVariant)));
			itemView.setTag(i);
			itemView.setOnClickListener(this::onItemClick);
			contentWrap.addView(itemView);
		}

		setContentView(contentView);

		return contentView;
	}

	public void show(View anchor){
		initView();
		contentView.measure(View.MeasureSpec.AT_MOST | V.dp(312), View.MeasureSpec.UNSPECIFIED);
		setWidth(contentView.getMeasuredWidth());
		setHeight(contentView.getMeasuredHeight());
		showAsDropDown(anchor);
	}

	private void onItemClick(View _item){
		CheckableTextView item=(CheckableTextView) _item;
		boolean checked=!item.isChecked();
		item.setChecked(checked);
		if(listener!=null)
			listener.onItemCheckedChanged((Integer)item.getTag(), checked);
	}

	private static class Item{
		public CharSequence title;
		public boolean checked;

		public Item(CharSequence title, boolean checked){
			this.checked=checked;
			this.title=title;
		}
	}

	public interface ItemCheckedChangedListener{
		void onItemCheckedChanged(int index, boolean checked);
	}
}
