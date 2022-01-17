package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;

import me.grishka.appkit.utils.BindableViewHolder;

public class ReblogOrReplyLineStatusDisplayItem extends StatusDisplayItem{
	public ReblogOrReplyLineStatusDisplayItem(Status status){
		super(status);
	}

	@Override
	public Type getType(){
		return Type.REBLOG_OR_REPLY_LINE;
	}

	public static class Holder extends BindableViewHolder<ReblogOrReplyLineStatusDisplayItem>{
		private final TextView text;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reblog_or_reply_line, parent);
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(ReblogOrReplyLineStatusDisplayItem item){
			if(item.status.reblog!=null){
				text.setText(itemView.getContext().getString(R.string.user_boosted, item.status.account.displayName));
			}
		}
	}
}
