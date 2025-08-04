package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Quote;
import org.joinmastodon.android.ui.RichTooltip;

import me.grishka.appkit.utils.V;

public class QuoteErrorStatusDisplayItem extends StatusDisplayItem{
	private final Quote.State state;

	public QuoteErrorStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, Quote.State state){
		super(parentID, parentFragment);
		this.state=state;
	}

	@Override
	public Type getType(){
		return Type.QUOTE_ERROR;
	}

	public static class Holder extends StatusDisplayItem.Holder<QuoteErrorStatusDisplayItem>{
		private final TextView text;
		private final Button learnMore;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_quote_error, parent);
			text=findViewById(R.id.text);
			learnMore=findViewById(R.id.btn_learn_more);
			learnMore.setOnClickListener(v->{
				new RichTooltip(v.getContext())
						.setMessage(R.string.quote_post_pending_explanation)
						.addButton(R.string.got_it, null)
						.show(v);
			});
		}

		@Override
		public void onBind(QuoteErrorStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), V.dp(8), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			if(item.state==Quote.State.PENDING){
				learnMore.setVisibility(View.VISIBLE);
				text.setText(R.string.quote_post_pending);
			}else{
				learnMore.setVisibility(View.GONE);
				text.setText(R.string.quote_post_unavailable);
			}
		}
	}
}
