package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;

public class ButtonStatusDisplayItem extends StatusDisplayItem{
	private String text;
	private int iconRes;
	private Runnable onClick;

	public ButtonStatusDisplayItem(String parentID, Callbacks callbacks, Context context, String text, int iconRes, Runnable onClick){
		super(parentID, callbacks, context);
		this.text=text;
		this.iconRes=iconRes;
		this.onClick=onClick;
	}

	@Override
	public Type getType(){
		return Type.BUTTON;
	}

	public static class Holder extends StatusDisplayItem.Holder<ButtonStatusDisplayItem>{
		private final View button;
		private final TextView text;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_button, parent);
			button=findViewById(R.id.button);
			text=findViewById(R.id.text);
			button.setOnClickListener(v->item.onClick.run());
		}

		@Override
		public void onBind(ButtonStatusDisplayItem item){
			text.setText(item.text);
			text.setCompoundDrawablesRelativeWithIntrinsicBounds(item.iconRes, 0, 0, 0);
		}

		@Override
		public boolean shouldHighlight(){
			return false;
		}

		@Override
		public boolean isEnabled(){
			return false;
		}
	}
}
