package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.collections.AccountCollection;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.viewholders.CollectionViewHolder;

import java.util.Map;
import java.util.function.Supplier;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CollectionStatusDisplayItem extends StatusDisplayItem{
	public final CollectionViewModel collection;

	public CollectionStatusDisplayItem(String parentID, Callbacks callbacks, Context context, AccountCollection collection, Map<String, Account> accounts){
		super(parentID, callbacks, context);
		this.collection=new CollectionViewModel(collection, accounts);
	}

	@Override
	public Type getType(){
		return Type.COLLECTION;
	}

	@Override
	public int getImageCount(){
		return collection.accounts.size();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return collection.avatarRequests.get(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<CollectionStatusDisplayItem> implements ImageLoaderViewHolder{
		private final CollectionViewHolder realHolder;

		public Holder(Context context, ViewGroup parent, String accountID){
			super(new FrameLayout(context));
			FrameLayout wrapper=(FrameLayout) itemView;
			realHolder=new CollectionViewHolder(context, parent, accountID, null, new CollectionViewHolder.AccountAndRelationshipProvider(){
				@Override
				public Account getAccount(String id){
					return null;
				}

				@Override
				public Relationship getRelationship(String id){
					return null;
				}
			});
			wrapper.addView(realHolder.itemView/*, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(69))*/);
			realHolder.itemView.setBackgroundResource(R.drawable.bg_inline_status);
			realHolder.itemView.setPaddingRelative(V.dp(4), 0, V.dp(12), 0);
			realHolder.menuButton.setVisibility(View.GONE);
			wrapper.setPaddingRelative(V.dp(64), 0, V.dp(16), V.dp(12));
		}

		@Override
		public void onBind(CollectionStatusDisplayItem item){
			realHolder.bind(item.collection);
		}

		@Override
		public void setImage(int index, Drawable image){
			realHolder.setImage(index, image);
		}

		@Override
		public void clearImage(int index){
			realHolder.clearImage(index);
		}
	}
}
