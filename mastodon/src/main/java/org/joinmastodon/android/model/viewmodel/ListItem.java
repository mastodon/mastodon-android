package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class ListItem<T>{
	public String title;
	public String subtitle;
	@StringRes
	public int titleRes;
	@StringRes
	public int subtitleRes;
	@DrawableRes
	public int iconRes;
	public int colorOverrideAttr;
	public boolean dividerAfter;
	public Runnable onClick;
	public boolean isEnabled=true;
	public T parentObject;

	public ListItem(String title, String subtitle, int iconRes, Runnable onClick, T parentObject, int colorOverrideAttr, boolean dividerAfter){
		this.title=title;
		this.subtitle=subtitle;
		this.iconRes=iconRes;
		this.colorOverrideAttr=colorOverrideAttr;
		this.dividerAfter=dividerAfter;
		this.onClick=onClick;
		this.parentObject=parentObject;
		if(onClick==null)
			isEnabled=false;
	}

	public ListItem(String title, String subtitle, Runnable onClick){
		this(title, subtitle, 0, onClick, null, 0, false);
	}

	public ListItem(String title, String subtitle, Runnable onClick, T parentObject){
		this(title, subtitle, 0, onClick, parentObject, 0, false);
	}

	public ListItem(String title, String subtitle, @DrawableRes int iconRes, Runnable onClick){
		this(title, subtitle, iconRes, onClick, null, 0, false);
	}

	public ListItem(String title, String subtitle, @DrawableRes int iconRes, Runnable onClick, T parentObject){
		this(title, subtitle, iconRes, onClick, parentObject, 0, false);
	}

	public ListItem(@StringRes int titleRes, @StringRes int subtitleRes, Runnable onClick){
		this(null, null, 0, onClick, null, 0, false);
		this.titleRes=titleRes;
		this.subtitleRes=subtitleRes;
	}

	public ListItem(@StringRes int titleRes, @StringRes int subtitleRes, Runnable onClick, int colorOverrideAttr, boolean dividerAfter){
		this(null, null, 0, onClick, null, colorOverrideAttr, dividerAfter);
		this.titleRes=titleRes;
		this.subtitleRes=subtitleRes;
	}

	public ListItem(@StringRes int titleRes, @StringRes int subtitleRes, @DrawableRes int iconRes, Runnable onClick){
		this(null, null, iconRes, onClick, null, 0, false);
		this.titleRes=titleRes;
		this.subtitleRes=subtitleRes;
	}

	public ListItem(@StringRes int titleRes, @StringRes int subtitleRes, @DrawableRes int iconRes, Runnable onClick, int colorOverrideAttr, boolean dividerAfter){
		this(null, null, iconRes, onClick, null, colorOverrideAttr, dividerAfter);
		this.titleRes=titleRes;
		this.subtitleRes=subtitleRes;
	}

	public int getItemViewType(){
		return colorOverrideAttr==0 ? R.id.list_item_simple : R.id.list_item_simple_tinted;
	}
}
