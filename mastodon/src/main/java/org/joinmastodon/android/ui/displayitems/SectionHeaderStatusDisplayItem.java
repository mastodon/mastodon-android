package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;

public class SectionHeaderStatusDisplayItem extends StatusDisplayItem{
	public final String title, buttonText;
	public final Runnable onButtonClick;

	public SectionHeaderStatusDisplayItem(BaseStatusListFragment parentFragment, String title, String buttonText, Runnable onButtonClick){
		super("", parentFragment);
		this.title=title;
		this.buttonText=buttonText;
		this.onButtonClick=onButtonClick;
	}

	@Override
	public Type getType(){
		return Type.SECTION_HEADER;
	}

	public static class Holder extends StatusDisplayItem.Holder<SectionHeaderStatusDisplayItem>{
		private final TextView title;
		private final Button actionBtn;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_section_header, parent);
			title=findViewById(R.id.title);
			actionBtn=findViewById(R.id.action_btn);
			actionBtn.setOnClickListener(v->item.onButtonClick.run());
		}

		@Override
		public void onBind(SectionHeaderStatusDisplayItem item){
			title.setText(item.title);
			if(item.onButtonClick!=null){
				actionBtn.setVisibility(View.VISIBLE);
				actionBtn.setText(item.buttonText);
			}else{
				actionBtn.setVisibility(View.GONE);
			}
		}

		@Override
		public boolean isEnabled(){
			return false;
		}
	}
}
