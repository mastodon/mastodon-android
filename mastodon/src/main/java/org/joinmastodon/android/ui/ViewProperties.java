package org.joinmastodon.android.ui;

import android.graphics.Typeface;
import android.os.Build;
import android.util.Property;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

public class ViewProperties{
	public static final Property<TextView, Integer> FONT_WEIGHT=new Property<>(Integer.class, "fontWeight"){
		@RequiresApi(api = Build.VERSION_CODES.P)
		@Override
		public Integer get(TextView object){
			return object.getTypeface().getWeight();
		}

		@RequiresApi(api = Build.VERSION_CODES.P)
		@Override
		public void set(TextView object, Integer value){
			// typeface objects are cached internally, I looked at AOSP sources to confirm that
			object.setTypeface(Typeface.create(null, value, false));
		}
	};

	public static final Property<TextView, Integer> TEXT_COLOR=new Property<>(Integer.class, "textColor"){
		@Override
		public Integer get(TextView object){
			return object.getCurrentTextColor();
		}

		@Override
		public void set(TextView object, Integer value){
			object.setTextColor(value);
		}
	};
}
