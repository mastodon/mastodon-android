package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NotificationHeaderStatusDisplayItem extends StatusDisplayItem{
	public final Notification notification;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence text;

	public NotificationHeaderStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Notification notification, String accountID){
		super(parentID, parentFragment);
		this.notification=notification;
		this.accountID=accountID;

		if(notification.type==Notification.Type.POLL){
			text=parentFragment.getString(R.string.poll_ended);
		}else{
			avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? notification.account.avatar : notification.account.avatarStatic, V.dp(50), V.dp(50));
			SpannableStringBuilder parsedName=new SpannableStringBuilder(notification.account.displayName);
			HtmlParser.parseCustomEmoji(parsedName, notification.account.emojis);
			emojiHelper.setText(parsedName);

			String[] parts=parentFragment.getString(switch(notification.type){
				case FOLLOW -> R.string.user_followed_you;
				case FOLLOW_REQUEST -> R.string.user_sent_follow_request;
				case REBLOG -> R.string.notification_boosted;
				case FAVORITE -> R.string.user_favorited;
				default -> throw new IllegalStateException("Unexpected value: "+notification.type);
			}).split("%s", 2);
			SpannableStringBuilder text=new SpannableStringBuilder();
			if(parts.length>1 && !TextUtils.isEmpty(parts[0]))
				text.append(parts[0]);
			text.append(parsedName, new TypefaceSpan("sans-serif-medium"), 0);
			if(parts.length==1){
				text.append(' ');
				text.append(parts[0]);
			}else if(!TextUtils.isEmpty(parts[1])){
				text.append(parts[1]);
			}
			this.text=text;
		}
	}

	@Override
	public Type getType(){
		return Type.NOTIFICATION_HEADER;
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

	public static class Holder extends StatusDisplayItem.Holder<NotificationHeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final ImageView icon, avatar;
		private final TextView text;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_notification_header, parent);
			icon=findViewById(R.id.icon);
			avatar=findViewById(R.id.avatar);
			text=findViewById(R.id.text);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
			avatar.setClipToOutline(true);
			avatar.setOnClickListener(this::onAvaClick);
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				avatar.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				text.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			if(index==0)
				avatar.setImageResource(R.drawable.image_placeholder);
			else
				ImageLoaderViewHolder.super.clearImage(index);
		}

		@Override
		public void onBind(NotificationHeaderStatusDisplayItem item){
			text.setText(item.text);
			avatar.setVisibility(item.notification.type==Notification.Type.POLL ? View.GONE : View.VISIBLE);
			// TODO use real icons
			icon.setImageResource(switch(item.notification.type){
				case FAVORITE -> R.drawable.ic_star_fill1_24px;
				case REBLOG -> R.drawable.ic_repeat_fill1_24px;
				case FOLLOW, FOLLOW_REQUEST -> R.drawable.ic_person_add_fill1_24px;
				case POLL -> R.drawable.ic_insert_chart_fill1_24px;
				default -> throw new IllegalStateException("Unexpected value: "+item.notification.type);
			});
			icon.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(item.parentFragment.getActivity(), switch(item.notification.type){
				case FAVORITE -> R.attr.colorFavorite;
				case REBLOG -> R.attr.colorBoost;
				case FOLLOW, FOLLOW_REQUEST -> R.attr.colorFollow;
				case POLL -> R.attr.colorPoll;
				default -> throw new IllegalStateException("Unexpected value: "+item.notification.type);
			})));
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.notification.account));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}
	}
}
