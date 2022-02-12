package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.ui.utils.UiUtils;

public class PollFooterStatusDisplayItem extends StatusDisplayItem{
	private Poll poll;

	public PollFooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Poll poll){
		super(parentID, parentFragment);
		this.poll=poll;
	}

	@Override
	public Type getType(){
		return Type.POLL_FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<PollFooterStatusDisplayItem>{
		private TextView text;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_poll_footer, parent);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(PollFooterStatusDisplayItem item){
			String text=item.parentFragment.getResources().getQuantityString(R.plurals.x_voters, item.poll.votersCount, item.poll.votersCount);
			if(item.poll.expiresAt!=null && !item.poll.expired){
				text+=" · "+UiUtils.formatTimeLeft(itemView.getContext(), item.poll.expiresAt);
			}else if(item.poll.expired){
				text+=" · "+item.parentFragment.getString(R.string.poll_closed);
			}
			this.text.setText(text);
		}
	}
}
