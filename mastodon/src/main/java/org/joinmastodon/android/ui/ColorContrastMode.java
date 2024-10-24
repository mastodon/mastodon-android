package org.joinmastodon.android.ui;

public enum ColorContrastMode{
	DEFAULT,
	MEDIUM,
	HIGH;

	public static ColorContrastMode fromContrastValue(float value){
		if(value>0.75f)
			return HIGH;
		if(value>0.25f)
			return MEDIUM;
		return DEFAULT;
	}
}
