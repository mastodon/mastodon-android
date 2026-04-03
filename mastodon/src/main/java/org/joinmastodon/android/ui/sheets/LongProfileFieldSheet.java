package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.widget.TextView;

import org.joinmastodon.android.R;

import androidx.annotation.NonNull;

public class LongProfileFieldSheet extends M3BottomSheet{
	public LongProfileFieldSheet(@NonNull Context context, CharSequence title, CharSequence value){
		super(context);
		setContentView(getLayoutInflater().inflate(R.layout.sheet_long_profile_field, null));
		TextView titleView=findViewById(R.id.title);
		TextView valueView=findViewById(R.id.value);
		titleView.setText(title);
		valueView.setText(value);
	}
}
