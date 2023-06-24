package org.joinmastodon.android.ui.displayitems;

import android.graphics.drawable.Drawable;

import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class AccountStatusDisplayItem extends StatusDisplayItem{
	public final AccountViewModel account;

	public AccountStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Account account){
		super(parentID, parentFragment);
		this.account=new AccountViewModel(account, parentFragment.getAccountID());
	}

	@Override
	public Type getType(){
		return Type.ACCOUNT;
	}

	@Override
	public int getImageCount(){
		return 1+account.emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index==0)
			return account.avaRequest;
		return account.emojiHelper.getImageRequest(index-1);
	}

	public static class Holder extends StatusDisplayItem.Holder<AccountStatusDisplayItem> implements ImageLoaderViewHolder{
		private final AccountViewHolder realHolder;

		public Holder(AccountViewHolder realHolder){
			super(realHolder.itemView);
			this.realHolder=realHolder;
			realHolder.setStyle(AccountViewHolder.AccessoryType.NONE, false);
		}

		@Override
		public void onBind(AccountStatusDisplayItem item){
			realHolder.bind(item.account);
		}

		@Override
		public void setImage(int index, Drawable image){
			realHolder.setImage(index, image);
		}

		@Override
		public void clearImage(int index){
			realHolder.clearImage(index);
		}

		@Override
		public void onClick(){
			realHolder.onClick();
		}
	}
}
