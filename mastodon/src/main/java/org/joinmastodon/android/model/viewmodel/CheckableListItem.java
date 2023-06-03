package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import java.util.function.Consumer;

public class CheckableListItem<T> extends ListItem<T>{
	public Style style;
	public boolean checked;
	public Consumer<Boolean> checkedChangeListener;

	public CheckableListItem(String title, String subtitle, Style style, boolean checked, int iconRes, Runnable onClick, T parentObject, boolean dividerAfter){
		super(title, subtitle, iconRes, onClick, parentObject, 0, dividerAfter);
		this.style=style;
		this.checked=checked;
	}

	public CheckableListItem(String title, String subtitle, Style style, boolean checked, Runnable onClick){
		this(title, subtitle, style, checked, 0, onClick, null, false);
	}

	public CheckableListItem(String title, String subtitle, Style style, boolean checked, Runnable onClick, T parentObject){
		this(title, subtitle, style, checked, 0, onClick, parentObject, false);
	}

	public CheckableListItem(String title, String subtitle, Style style, boolean checked, int iconRes, Runnable onClick){
		this(title, subtitle, style, checked, iconRes, onClick, null, false);
	}

	public CheckableListItem(String title, String subtitle, Style style, boolean checked, int iconRes, Runnable onClick, T parentObject){
		this(title, subtitle, style, checked, iconRes, onClick, parentObject, false);
	}

	public CheckableListItem(int titleRes, int subtitleRes, Style style, boolean checked, Runnable onClick){
		this(titleRes, subtitleRes, style, checked, 0, onClick, false);
	}

	public CheckableListItem(int titleRes, int subtitleRes, Style style, boolean checked, Runnable onClick, boolean dividerAfter){
		this(titleRes, subtitleRes, style, checked, 0, onClick, dividerAfter);
	}

	public CheckableListItem(int titleRes, int subtitleRes, Style style, boolean checked, int iconRes, Runnable onClick){
		this(titleRes, subtitleRes, style, checked, iconRes, onClick, false);
	}

	public CheckableListItem(int titleRes, int subtitleRes, Style style, boolean checked, int iconRes, Runnable onClick, boolean dividerAfter){
		super(titleRes, subtitleRes, iconRes, onClick, 0, dividerAfter);
		this.style=style;
		this.checked=checked;
	}

	@Override
	public int getItemViewType(){
		return switch(style){
			case CHECKBOX -> R.id.list_item_checkbox;
			case RADIO -> R.id.list_item_radio;
			case SWITCH -> R.id.list_item_switch;
		};
	}

	public void setChecked(boolean checked){
		this.checked=checked;
	}

	public void toggle(){
		checked=!checked;
	}

	public enum Style{
		CHECKBOX,
		RADIO,
		SWITCH
	}
}
