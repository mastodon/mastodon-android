package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.views.CheckableRelativeLayout;

import java.time.Instant;
import java.util.function.Predicate;

public class CheckableHeaderStatusDisplayItem extends HeaderStatusDisplayItem{
	public CheckableHeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status, String extraText){
		super(parentID, user, createdAt, parentFragment, accountID, status, extraText);
	}

	@Override
	public Type getType(){
		return Type.HEADER_CHECKABLE;
	}

	public static class Holder extends HeaderStatusDisplayItem.Holder{
		private final View checkbox;
		private final CheckableRelativeLayout view;
		private Predicate<Holder> isChecked;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header_checkable, parent);
			checkbox=findViewById(R.id.checkbox);
			view=(CheckableRelativeLayout) itemView;
			checkbox.setBackground(new CheckBox(activity).getButtonDrawable());
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			super.onBind(item);
			if(isChecked!=null){
				view.setChecked(isChecked.test(this));
			}
		}

		public void setIsChecked(Predicate<Holder> isChecked){
			this.isChecked=isChecked;
		}
	}
}
