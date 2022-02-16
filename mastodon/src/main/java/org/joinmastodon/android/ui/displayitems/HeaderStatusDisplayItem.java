package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Outline;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
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
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public final Status status;
	private boolean hasVisibilityToggle;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status){
		super(parentID, parentFragment);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(user.avatar);
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		this.status=status;
		HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
		hasVisibilityToggle=status.sensitive || !TextUtils.isEmpty(status.spoilerText);
		if(!hasVisibilityToggle && !status.mediaAttachments.isEmpty()){
			for(Attachment att:status.mediaAttachments){
				if(att.type!=Attachment.Type.AUDIO){
					hasVisibilityToggle=true;
					break;
				}
			}
		}
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
		private final ImageView avatar, more, visibility;

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
			visibility=findViewById(R.id.visibility);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);
			visibility.setOnClickListener(v->item.parentFragment.onVisibilityIconClick(this));
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			username.setText('@'+item.user.acct);
			timestamp.setText(UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt));
			visibility.setVisibility(item.hasVisibilityToggle ? View.VISIBLE : View.GONE);
			if(item.hasVisibilityToggle){
				visibility.setImageResource(item.status.spoilerRevealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
			}
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
				name.invalidate();
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
