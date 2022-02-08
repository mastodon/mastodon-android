package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.BindableViewHolder;

public class ReblogOrReplyLineStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;

	public ReblogOrReplyLineStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, CharSequence text){
		super(parentID, parentFragment);
		this.text=text;
	}

	@Override
	public Type getType(){
		return Type.REBLOG_OR_REPLY_LINE;
	}

	public static class Holder extends StatusDisplayItem.Holder<ReblogOrReplyLineStatusDisplayItem>{
		private final TextView text;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reblog_or_reply_line, parent);
			text=findViewById(R.id.text);
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N)
				UiUtils.fixCompoundDrawableTintOnAndroid6(text, R.color.text_secondary);
		}

		@Override
		public void onBind(ReblogOrReplyLineStatusDisplayItem item){
			text.setText(item.text);
		}
	}
}
