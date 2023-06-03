package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class AccountStatusDisplayItem extends StatusDisplayItem{
	public final Account account;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence parsedName;
	public ImageLoaderRequest avaRequest;

	public AccountStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Account account){
		super(parentID, parentFragment);
		this.account=account;
		if(AccountSessionManager.get(parentFragment.getAccountID()).getLocalPreferences().customEmojiInNames)
			parsedName=HtmlParser.parseCustomEmoji(account.displayName, account.emojis);
		else
			parsedName=account.displayName;
		emojiHelper.setText(parsedName);
		if(!TextUtils.isEmpty(account.avatar))
			avaRequest=new UrlImageLoaderRequest(account.avatar, V.dp(50), V.dp(50));
	}

	@Override
	public Type getType(){
		return Type.ACCOUNT;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index==0)
			return avaRequest;
		return emojiHelper.getImageRequest(index-1);
	}

	public static class Holder extends StatusDisplayItem.Holder<AccountStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, username;
		private final ImageView photo;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_account, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			photo=findViewById(R.id.photo);

			photo.setOutlineProvider(OutlineProviders.roundedRect(12));
			photo.setClipToOutline(true);
		}

		@Override
		public void onBind(AccountStatusDisplayItem item){
			name.setText(item.parsedName);
			username.setText("@"+item.account.acct);
		}

		@Override
		public void setImage(int index, Drawable image){
			if(image instanceof Animatable && !((Animatable) image).isRunning())
				((Animatable) image).start();
			if(index==0){
				photo.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				name.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
