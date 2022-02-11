package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Outline;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private Fragment parentFragment;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID){
		super(parentID, parentFragment);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(user.avatar);
		this.parentFragment=parentFragment;
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>0){
			return emojiHelper.getImageRequest(index-1);
		}
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, username, timestamp;
		private final ImageView avatar, more;

		private static final ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			timestamp=findViewById(R.id.timestamp);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			username.setText('@'+item.user.acct);
			timestamp.setText(UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt));
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
			}else{
				avatar.setImageDrawable(drawable);
			}
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}

		private void onMoreClick(View v){

		}
	}
}
