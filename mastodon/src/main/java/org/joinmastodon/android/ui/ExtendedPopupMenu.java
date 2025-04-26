package org.joinmastodon.android.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.viewholders.SectionHeaderListItemViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ExtendedPopupMenu extends PopupWindow{
	private UsableRecyclerView list;

	public <T> ExtendedPopupMenu(Context context, List<ListItem<T>> items){
		super(context, null, 0, R.style.Widget_Mastodon_PopupMenu);
		setWidth(V.dp(200));
		setElevation(V.dp(3));
		setOutsideTouchable(true);
		setFocusable(true);
		setInputMethodMode(INPUT_METHOD_NOT_NEEDED);
		list=new UsableRecyclerView(context);
		list.setLayoutManager(new LinearLayoutManager(context));
		list.setAdapter(new ReducedPaddingItemsAdapter<>(items));
		list.setClipToPadding(false);
		setContentView(list);
	}

	@Override
	public void showAsDropDown(View anchor, int xoff, int yoff, int gravity){
		super.showAsDropDown(anchor, xoff, yoff, gravity);
		View bgView=(View) list.getParent();
		list.setPadding(0, bgView.getPaddingTop(), 0, bgView.getPaddingBottom());
		bgView.setPadding(0, 0, 0, 0);
	}

	private static class ReducedPaddingItemsAdapter<T> extends GenericListItemsAdapter<T>{
		public ReducedPaddingItemsAdapter(List<ListItem<T>> listItems){
			super(listItems);
		}

		@NonNull
		@Override
		public ListItemViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			ListItemViewHolder<?> holder=super.onCreateViewHolder(parent, viewType);
			int padH=V.dp(12), padV=V.dp(8);
			holder.itemView.setPadding(padH, padV, padH, padV);
			if(holder instanceof SectionHeaderListItemViewHolder shh){
				shh.setPopupMenuStyle();
			}else{
				View icon=holder.itemView.findViewById(R.id.icon);
				((ViewGroup.MarginLayoutParams)icon.getLayoutParams()).setMarginEnd(padH);
			}
			return holder;
		}
	}
}
