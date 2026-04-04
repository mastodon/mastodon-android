package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import java.util.function.Consumer;

public class ListItemWithTrailingIcon<T> extends ListItem<T>{
	public ListItemWithTrailingIcon(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick){
		super(title, subtitle, iconRes, onClick);
	}

	public ListItemWithTrailingIcon(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick, T parentObject){
		super(title, subtitle, iconRes, onClick, parentObject);
	}

	public ListItemWithTrailingIcon(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick, T parentObject, int colorOverrideAttr, boolean dividerAfter){
		super(title, subtitle, iconRes, onClick, parentObject, colorOverrideAttr, dividerAfter);
	}

	public ListItemWithTrailingIcon(String title, String subtitle, Consumer<ListItem<T>> onClick){
		super(title, subtitle, onClick);
	}

	public ListItemWithTrailingIcon(String title, String subtitle, Consumer<ListItem<T>> onClick, T parentObject){
		super(title, subtitle, onClick, parentObject);
	}

	public ListItemWithTrailingIcon(int titleRes, int subtitleRes, int iconRes, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, iconRes, onClick);
	}

	public ListItemWithTrailingIcon(int titleRes, int subtitleRes, int iconRes, Consumer<ListItem<T>> onClick, int colorOverrideAttr, boolean dividerAfter){
		super(titleRes, subtitleRes, iconRes, onClick, colorOverrideAttr, dividerAfter);
	}

	public ListItemWithTrailingIcon(int titleRes, int subtitleRes, int iconRes, T parentObject, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, iconRes, parentObject, onClick);
	}

	public ListItemWithTrailingIcon(int titleRes, int subtitleRes, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, onClick);
	}

	public ListItemWithTrailingIcon(int titleRes, int subtitleRes, Consumer<ListItem<T>> onClick, int colorOverrideAttr, boolean dividerAfter){
		super(titleRes, subtitleRes, onClick, colorOverrideAttr, dividerAfter);
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_simple_trailing_icon;
	}
}
