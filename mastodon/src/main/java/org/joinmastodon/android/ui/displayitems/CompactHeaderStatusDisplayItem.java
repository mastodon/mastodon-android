package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CompactHeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public final Status status;

	public CompactHeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, Callbacks callbacks, Context context, String accountID, Status status){
		super(parentID, callbacks, context);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? user.avatar : user.avatarStatic, V.dp(50), V.dp(50));
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		this.status=status;
		if(GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
	}

	@Override
	public Type getType(){
		return Type.HEADER_COMPACT;
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

	public static class Holder extends StatusDisplayItem.Holder<CompactHeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, timeAndUsername;
		private final ImageView avatar;
		private final View clickableThing;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header_compact, parent);
			name=findViewById(R.id.name);
			timeAndUsername=findViewById(R.id.time_and_username);
			avatar=findViewById(R.id.avatar);
			clickableThing=findViewById(R.id.clickable_thing);
			if(clickableThing!=null)
				clickableThing.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
			avatar.setClipToOutline(true);
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(CompactHeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			String time;
			if(item.status==null || item.status.editedAt==null)
				time=UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt);
			else
				time=item.context.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt));

			timeAndUsername.setText(time+" · @"+item.user.acct);
//			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(6) : V.dp(4));
			if(clickableThing!=null){
				clickableThing.setContentDescription(item.context.getString(R.string.avatar_description, item.user.acct));
			}
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 0 : 48), 0, 0, 0);
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
			if(index==0){
				avatar.setImageResource(R.drawable.image_placeholder);
				return;
			}
			setImage(index, null);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go((Activity) item.context, ProfileFragment.class, args);
		}
	}
}
