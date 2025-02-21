package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.ViewGroup;

import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.viewmodel.CardViewModel;
import org.joinmastodon.android.ui.viewholders.LinkCardHolder;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class LinkCardStatusDisplayItem extends StatusDisplayItem implements LinkCardHolder.LinkCardProvider{
	private final Status status;
	private final CardViewModel cardViewModel;

	public LinkCardStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.status=status;
		int size=shouldUseLargeCard() ? 1000 : 192;
		cardViewModel=new CardViewModel(status.card, size, size, status, parentFragment.getAccountID());
	}

	private boolean shouldUseLargeCard(){
		return status.card.type==Card.Type.VIDEO || (status.card.image!=null && status.card.width>status.card.height);
	}

	@Override
	public Type getType(){
		return shouldUseLargeCard() ? Type.CARD_LARGE : Type.CARD_COMPACT;
	}

	@Override
	public int getImageCount(){
		return cardViewModel.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return cardViewModel.getImageRequest(index);
	}

	@Override
	public CardViewModel getCard(){
		return cardViewModel;
	}

	public static class Holder extends LinkCardHolder<LinkCardStatusDisplayItem>{

		public Holder(Activity context, ViewGroup parent, boolean isLarge, String accountID){
			super(context, parent, isLarge, accountID);
		}

		@Override
		public void onBind(LinkCardStatusDisplayItem item){
			super.onBind(item);
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), item.status.poll==null ? 0 : V.dp(12), itemView.getPaddingEnd(), itemView.getPaddingBottom());
		}
	}
}
