package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;

import androidx.annotation.LayoutRes;
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
	boolean needBottomPadding;
	public boolean isPinned;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, Callbacks callbacks, Context context, String accountID, Status status){
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
		if(status!=null){
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
		private final TextView name, time, username, badge;
		private final ImageView avatar;
		private final View clickableThing;

		public Holder(Activity activity, ViewGroup parent){
			this(activity, R.layout.display_item_header, parent);
		}

		protected Holder(Activity activity, @LayoutRes int layout, ViewGroup parent){
			super(activity, layout, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			time=findViewById(R.id.time);
			avatar=findViewById(R.id.avatar);
			clickableThing=findViewById(R.id.clickable_thing);
			badge=findViewById(R.id.badge);
			if(clickableThing!=null)
				clickableThing.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(OutlineProviders.OVAL);
			avatar.setClipToOutline(true);
			if(badge!=null){
				badge.setOutlineProvider(OutlineProviders.roundedRect(8));
				badge.setClipToOutline(true);
			}

		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			String timeStr;
			if(item.status==null || item.status.editedAt==null)
				timeStr=UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt);
			else
				timeStr=item.context.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt));

			username.setText("@"+item.user.acct);
			String visibilityStr="";
			if(item.status!=null){
				if(item.status.visibility==StatusPrivacy.PRIVATE){
					visibilityStr=" "+item.context.getString(R.string.post_label_to_followers);
				}else if(item.status.visibility==StatusPrivacy.DIRECT){
					boolean hasSelf=false;
					String selfID=AccountSessionManager.get(item.accountID).self.id;
					for(Mention m:item.status.mentions){
						if(m.id.equals(selfID)){
							hasSelf=true;
							break;
						}
					}
					if(hasSelf){
						if(item.status.mentions.size()==1)
							visibilityStr=" "+item.context.getString(R.string.post_label_to_you);
						else
							visibilityStr=" "+item.context.getString(R.string.post_label_to_you_and_others, item.status.mentions.size()-1);
					}else if(!item.status.mentions.isEmpty()){
						Mention firstMention=item.status.mentions.get(0);
						Account firstAccount=item.callbacks.getKnownAccount(firstMention.id);
						String firstAccountName=firstAccount==null ? firstMention.username : firstAccount.displayName;
						if(item.status.mentions.size()==1)
							visibilityStr=" "+item.context.getString(R.string.post_label_to_user, firstAccountName);
						else
							visibilityStr=" "+item.context.getString(R.string.post_label_to_user_and_others, firstAccountName, item.status.mentions.size()-1);
					}
				}
			}
			time.setText(visibilityStr+" • "+timeStr);
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(6) : V.dp(4));
			if(clickableThing!=null){
				clickableThing.setContentDescription(item.context.getString(R.string.avatar_description, item.user.acct));
			}
			if(badge!=null){
				if(item.isPinned){
					badge.setVisibility(View.VISIBLE);
					badge.setText(R.string.pinned_post);
					badge.setBackgroundResource(R.drawable.bg_m3_surface1);
					int textColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurface);
					badge.setTextColor(textColor);
					badge.setCompoundDrawableTintList(ColorStateList.valueOf(textColor));
					badge.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pin_16px, 0, 0, 0);
				}else{
					badge.setVisibility(View.GONE);
				}
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
			args.putString("followReferrer", "status");
			Nav.go((Activity) item.context, ProfileFragment.class, args);
		}
	}
}
