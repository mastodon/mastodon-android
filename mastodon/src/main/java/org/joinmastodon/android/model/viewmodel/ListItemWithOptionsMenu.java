package org.joinmastodon.android.model.viewmodel;

import android.view.Menu;
import android.view.MenuItem;

import org.joinmastodon.android.R;

import java.util.function.Consumer;

public class ListItemWithOptionsMenu<T> extends ListItem<T>{
	public OptionsMenuListener<T> listener;

	public ListItemWithOptionsMenu(String title, String subtitle, OptionsMenuListener<T> listener, int iconRes, Consumer<ListItemWithOptionsMenu<T>> onClick, T parentObject, boolean dividerAfter){
		super(title, subtitle, iconRes, (Consumer<ListItem<T>>)(Object)onClick, parentObject, 0, dividerAfter);
		this.listener=listener;
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_options;
	}

	public void performConfigureMenu(Menu menu){
		listener.onConfigureListItemOptionsMenu(this, menu);
	}

	public void performItemSelected(MenuItem item){
		listener.onListItemOptionSelected(this, item);
	}

	public interface OptionsMenuListener<T>{
		void onConfigureListItemOptionsMenu(ListItemWithOptionsMenu<T> item, Menu menu);
		void onListItemOptionSelected(ListItemWithOptionsMenu<T> item, MenuItem menuItem);
	}
}
