package org.joinmastodon.android.ui.viewcontrollers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;
import java.util.function.Consumer;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class DropdownSubmenuController{
	protected List<Item<?>> items;
	protected LinearLayout contentView;
	protected UsableRecyclerView list;
	protected TextView backItem;
	protected final ToolbarDropdownMenuController dropdownController;
	protected MergeRecyclerAdapter mergeAdapter;
	protected ItemsAdapter itemsAdapter;

	public DropdownSubmenuController(ToolbarDropdownMenuController dropdownController){
		this.dropdownController=dropdownController;
	}

	protected abstract CharSequence getBackItemTitle();
	public void onDismiss(){}

	protected void createView(){
		contentView=new LinearLayout(dropdownController.getActivity());
		contentView.setOrientation(LinearLayout.VERTICAL);
		CharSequence backTitle=getBackItemTitle();
		if(!TextUtils.isEmpty(backTitle)){
			backItem=(TextView) dropdownController.getActivity().getLayoutInflater().inflate(R.layout.item_dropdown_menu, contentView, false);
			((LinearLayout.LayoutParams) backItem.getLayoutParams()).topMargin=V.dp(8);
			backItem.setText(backTitle);
			backItem.setCompoundDrawablesRelativeWithIntrinsicBounds(me.grishka.appkit.R.drawable.ic_arrow_back, 0, 0, 0);
			backItem.setBackground(UiUtils.getThemeDrawable(dropdownController.getActivity(), android.R.attr.selectableItemBackground));
			backItem.setOnClickListener(v->dropdownController.popSubmenuController());
			backItem.setAccessibilityDelegate(new View.AccessibilityDelegate(){
				@Override
				public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info){
					super.onInitializeAccessibilityNodeInfo(host, info);
					info.setText(info.getText()+". "+host.getResources().getString(R.string.back));
				}
			});
			contentView.addView(backItem);
		}
		list=new UsableRecyclerView(dropdownController.getActivity());
		list.setLayoutManager(new LinearLayoutManager(dropdownController.getActivity()));
		itemsAdapter=new ItemsAdapter();
		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(itemsAdapter);
		list.setAdapter(mergeAdapter);
		list.setPadding(0, backItem!=null ? 0 : V.dp(8), 0, V.dp(8));
		list.setClipToPadding(false);
		list.setItemAnimator(new BetterItemAnimator());
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private final Paint paint=new Paint();
			{
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(V.dp(1));
				paint.setColor(UiUtils.getThemeColor(dropdownController.getActivity(), R.attr.colorM3OutlineVariant));
			}

			@Override
			public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				for(int i=0;i<parent.getChildCount();i++){
					View view=parent.getChildAt(i);
					if(parent.getChildViewHolder(view) instanceof ItemHolder ih && ih.getItem().dividerBefore){
						paint.setAlpha(Math.round(view.getAlpha()*255));
						float y=view.getTop()-V.dp(8)-paint.getStrokeWidth()/2f;
						c.drawLine(0, y, parent.getWidth(), y, paint);
					}
				}
			}

			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(parent.getChildViewHolder(view) instanceof ItemHolder ih && ih.getItem().dividerBefore){
					outRect.top=V.dp(17);
				}
			}
		});
		contentView.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	public View getView(){
		if(contentView==null)
			createView();
		return contentView;
	}

	protected HideableSingleViewRecyclerAdapter createEmptyView(@DrawableRes int icon, @StringRes int title, @StringRes int subtitle){
		View view=dropdownController.getActivity().getLayoutInflater().inflate(R.layout.popup_menu_empty, list, false);
		ImageView iconView=view.findViewById(R.id.icon);
		TextView titleView=view.findViewById(R.id.title);
		TextView subtitleView=view.findViewById(R.id.subtitle);
		iconView.setImageResource(icon);
		titleView.setText(title);
		subtitleView.setText(subtitle);
		return new HideableSingleViewRecyclerAdapter(view);
	}

	protected final class Item<T>{
		public final String title;
		public final boolean hasSubmenu;
		public final boolean dividerBefore;
		public final T parentObject;
		public final Consumer<Item<T>> onClick;

		public Item(String title, boolean hasSubmenu, boolean dividerBefore, T parentObject, Consumer<Item<T>> onClick){
			this.title=title;
			this.hasSubmenu=hasSubmenu;
			this.dividerBefore=dividerBefore;
			this.parentObject=parentObject;
			this.onClick=onClick;
		}

		public Item(String title, boolean hasSubmenu, boolean dividerBefore, Consumer<Item<T>> onClick){
			this(title, hasSubmenu, dividerBefore, null, onClick);
		}

		public Item(@StringRes int titleRes, boolean hasSubmenu, boolean dividerBefore, Consumer<Item<T>> onClick){
			this(dropdownController.getActivity().getString(titleRes), hasSubmenu, dividerBefore, null, onClick);
		}

		private void performClick(){
			onClick.accept(this);
		}
	}

	protected class ItemsAdapter extends RecyclerView.Adapter<ItemHolder>{

		@NonNull
		@Override
		public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new ItemHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull ItemHolder holder, int position){
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}
	}

	private class ItemHolder extends BindableViewHolder<Item<?>> implements UsableRecyclerView.Clickable{
		private final TextView text;

		public ItemHolder(){
			super(dropdownController.getActivity(), R.layout.item_dropdown_menu, list);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(Item<?> item){
			text.setText(item.title);
			text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, item.hasSubmenu ? R.drawable.ic_arrow_right_24px : 0, 0);
		}

		@Override
		public void onClick(){
			item.performClick();
		}
	}
}
