package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.views.BottomSheet;
import me.grishka.appkit.views.UsableRecyclerView;

public class ListItemsSheet extends BottomSheet{
	private UsableRecyclerView list;
	private GenericListItemsAdapter<?> adapter;
	private ArrayList<ListItem<?>> items=new ArrayList<>();

	public ListItemsSheet(@NonNull Context context){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_recyclerview, null);
		setContentView(content);
		list=content.findViewById(R.id.list);
		list.setNestedScrollingEnabled(true);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		((NestedRecyclerScrollView)content).setScrollableChildSupplier(()->list);

		//noinspection rawtypes,unchecked
		adapter=new GenericListItemsAdapter(items);
		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(adapter);
		list.setAdapter(mergeAdapter);
		list.setLayoutManager(new LinearLayoutManager(context));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}

	@Override
	public void dismiss(){
		// Prevents this sheet messing with the soft keyboard
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		super.dismiss();
	}

	public void add(List<ListItem<?>> items){
		this.items.addAll(items);
		adapter.notifyItemRangeInserted(this.items.size()-items.size(), items.size());
	}

	public void add(ListItem<?> item){
		items.add(item);
		adapter.notifyItemInserted(items.size()-1);
	}
}
