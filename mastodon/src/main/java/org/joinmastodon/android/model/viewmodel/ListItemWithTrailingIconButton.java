package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import java.util.function.Consumer;

public class ListItemWithTrailingIconButton<T> extends ListItem<T>{
	public Consumer<ListItemWithTrailingIconButton<T>> buttonOnClickListener;
	public String buttonContentDescription;
	public int iconColorAttr;

	public ListItemWithTrailingIconButton(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick){
		super(title, subtitle, iconRes, onClick);
	}

	public ListItemWithTrailingIconButton(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick, T parentObject){
		super(title, subtitle, iconRes, onClick, parentObject);
	}

	public ListItemWithTrailingIconButton(String title, String subtitle, int iconRes, Consumer<ListItem<T>> onClick, T parentObject, int colorOverrideAttr, boolean dividerAfter){
		super(title, subtitle, iconRes, onClick, parentObject, colorOverrideAttr, dividerAfter);
	}

	public ListItemWithTrailingIconButton(String title, String subtitle, Consumer<ListItem<T>> onClick){
		super(title, subtitle, onClick);
	}

	public ListItemWithTrailingIconButton(String title, String subtitle, Consumer<ListItem<T>> onClick, T parentObject){
		super(title, subtitle, onClick, parentObject);
	}

	public ListItemWithTrailingIconButton(int titleRes, int subtitleRes, int iconRes, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, iconRes, onClick);
	}

	public ListItemWithTrailingIconButton(int titleRes, int subtitleRes, int iconRes, Consumer<ListItem<T>> onClick, int colorOverrideAttr, boolean dividerAfter){
		super(titleRes, subtitleRes, iconRes, onClick, colorOverrideAttr, dividerAfter);
	}

	public ListItemWithTrailingIconButton(int titleRes, int subtitleRes, int iconRes, T parentObject, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, iconRes, parentObject, onClick);
	}

	public ListItemWithTrailingIconButton(int titleRes, int subtitleRes, Consumer<ListItem<T>> onClick){
		super(titleRes, subtitleRes, onClick);
	}

	public ListItemWithTrailingIconButton(int titleRes, int subtitleRes, Consumer<ListItem<T>> onClick, int colorOverrideAttr, boolean dividerAfter){
		super(titleRes, subtitleRes, onClick, colorOverrideAttr, dividerAfter);
	}

	@Override
	public int getItemViewType(){
		return R.id.list_item_simple_trailing_icon_button;
	}

	public void invokeOnClickListener(){
		buttonOnClickListener.accept(this);
	}
}
