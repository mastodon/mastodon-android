package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Quote;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;

import me.grishka.appkit.utils.V;

public class NestedQuoteStatusDisplayItem extends StatusDisplayItem{
	public Status status;
	public final String statusID;
	public final Quote quote;

	public NestedQuoteStatusDisplayItem(String parentID, Callbacks callbacks, Context context, String statusID, Quote quote){
		super(parentID, callbacks, context);
		this.statusID=statusID;
		this.quote=quote;
		status=quote.quotedStatus;
	}

	@Override
	public Type getType(){
		return Type.NESTED_QUOTE;
	}

	public static class Holder extends StatusDisplayItem.Holder<NestedQuoteStatusDisplayItem>{
		private final TextView text;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_nested_quote, parent);
			text=findViewById(R.id.text);
			text.setOutlineProvider(OutlineProviders.roundedRect(8));
			text.setClipToOutline(true);
		}

		@Override
		public void onBind(NestedQuoteStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), V.dp(8), itemView.getPaddingEnd(), itemView.getPaddingBottom());

			if(item.status!=null)
				text.setText(text.getContext().getString(R.string.nested_quote, item.status.account.getDisplayUsername()));
			else if(item.quote.state==Quote.State.ACCEPTED)
				text.setText(R.string.loading);
			else if(item.quote.state==Quote.State.PENDING)
				text.setText(R.string.quote_post_pending);
			else
				text.setText(R.string.quote_post_unavailable);
		}
	}
}
