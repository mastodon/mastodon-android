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
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredLinkSpan;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NotificationHeaderStatusDisplayItem extends StatusDisplayItem{
	public final NotificationViewModel notification;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence text;
	private List<Account> accounts;
	private List<ImageLoaderRequest> avaRequests;

	public NotificationHeaderStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, NotificationViewModel notification, String accountID){
		super(parentID, parentFragment);
		this.notification=notification;
		this.accountID=accountID;

		if(notification.accounts.size()<=6){
			accounts=notification.accounts;
		}else{
			accounts=notification.accounts.subList(0, 6);
		}
		avaRequests=accounts.stream()
				.map(a->new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? a.avatar : a.avatarStatic, V.dp(50), V.dp(50)))
				.collect(Collectors.toList());

		if(notification.notification.type==NotificationType.POLL && AccountSessionManager.getInstance().isSelf(accountID, notification.accounts.get(0))){
			text=parentFragment.getString(R.string.own_poll_ended);
		}else{
			Account account=notification.accounts.get(0);
			SpannableStringBuilder parsedName=new SpannableStringBuilder(account.displayName);
			if(GlobalUserPreferences.customEmojiInNames)
				HtmlParser.parseCustomEmoji(parsedName, account.emojis);
			emojiHelper.setText(parsedName);

			String text;
			if(accounts.size()>1 && notification.notification.type.canBeGrouped()){
				text=parentFragment.getResources().getQuantityString(switch(notification.notification.type){
					case FAVORITE -> R.plurals.user_and_x_more_favorited;
					case REBLOG -> R.plurals.user_and_x_more_boosted;
					case FOLLOW -> R.plurals.user_and_x_more_followed;
					default -> throw new IllegalStateException("Unexpected value: " + notification.notification.type);
				}, notification.notification.notificationsCount-1, "{{name}}", notification.notification.notificationsCount-1);
			}else if(notification.notification.type==NotificationType.POLL){
				if(notification.status==null || notification.status.poll==null){
					text="???";
				}else{
					int count=notification.status.poll.votersCount-1;
					text=parentFragment.getResources().getQuantityString(R.plurals.poll_ended_x_voters, count, "{{name}}", count);
				}
			}else{
				text=parentFragment.getString(switch(notification.notification.type){
					case FOLLOW -> R.string.user_followed_you;
					case FOLLOW_REQUEST -> R.string.user_sent_follow_request;
					case REBLOG -> R.string.notification_boosted;
					case FAVORITE -> R.string.user_favorited;
					case UPDATE -> R.string.user_edited_post;
					default -> throw new IllegalStateException("Unexpected value: "+notification.notification.type);
				}, "{{name}}");
			}
			String[] parts=text.split(Pattern.quote("{{name}}"), 2);
			SpannableStringBuilder formattedText=new SpannableStringBuilder();
			if(parts.length>1 && !TextUtils.isEmpty(parts[0]))
				formattedText.append(parts[0]);
			formattedText.append(parsedName, new TypefaceSpan("sans-serif-medium"), 0);
			formattedText.setSpan(new NonColoredLinkSpan(null, s->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("profileAccount", Parcels.wrap(account));
				Nav.go(parentFragment.getActivity(), ProfileFragment.class, args);
			}, LinkSpan.Type.CUSTOM, null, null, null), formattedText.length()-parsedName.length(), formattedText.length(), 0);
			if(parts.length==1){
				formattedText.append(' ');
				formattedText.append(parts[0]);
			}else if(!TextUtils.isEmpty(parts[1])){
				formattedText.append(parts[1]);
			}
			this.text=formattedText;
		}
	}

	@Override
	public Type getType(){
		return Type.NOTIFICATION_HEADER;
	}

	@Override
	public int getImageCount(){
		return avaRequests.size()+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>=avaRequests.size()){
			return emojiHelper.getImageRequest(index-avaRequests.size());
		}
		return avaRequests.get(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<NotificationHeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final ImageView icon;
		private final TextView text;
		private final ImageView[] avatars;
		private final View avatarsContainer;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_notification_header, parent);
			icon=findViewById(R.id.icon);
			text=findViewById(R.id.text);
			avatars=new ImageView[]{
					findViewById(R.id.avatar1),
					findViewById(R.id.avatar2),
					findViewById(R.id.avatar3),
					findViewById(R.id.avatar4),
					findViewById(R.id.avatar5),
					findViewById(R.id.avatar6),
			};
			avatarsContainer=findViewById(R.id.avatars);

			int i=0;
			for(ImageView avatar:avatars){
				avatar.setOutlineProvider(OutlineProviders.roundedRect(6));
				avatar.setClipToOutline(true);
				avatar.setOnClickListener(this::onAvaClick);
				avatar.setTag(i);
				i++;
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index<item.avaRequests.size()){
				avatars[index].setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-item.avaRequests.size(), image);
				text.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			if(index<item.avaRequests.size())
				avatars[index].setImageResource(R.drawable.image_placeholder);
			else
				ImageLoaderViewHolder.super.clearImage(index);
		}

		@Override
		public void onBind(NotificationHeaderStatusDisplayItem item){
			text.setText(item.text);

			if(item.notification.notification.type==NotificationType.POLL || item.notification.notification.type==NotificationType.UPDATE){
				avatarsContainer.setVisibility(View.GONE);
			}else{
				avatarsContainer.setVisibility(View.VISIBLE);
				for(int i=0;i<avatars.length;i++){
					if(i>=item.accounts.size()){
						avatars[i].setVisibility(View.GONE);
					}else{
						avatars[i].setVisibility(View.VISIBLE);
						avatars[i].setContentDescription(item.parentFragment.getString(R.string.avatar_description, item.notification.accounts.get(i).acct));
					}
				}
			}
			icon.setImageResource(switch(item.notification.notification.type){
				case FAVORITE -> R.drawable.ic_star_fill1_24px;
				case REBLOG -> R.drawable.ic_repeat_fill1_24px;
				case FOLLOW, FOLLOW_REQUEST -> R.drawable.ic_person_add_fill1_24px;
				case POLL -> R.drawable.ic_insert_chart_fill1_24px;
				case UPDATE -> R.drawable.ic_edit_24px;
				default -> throw new IllegalStateException("Unexpected value: "+item.notification.notification.type);
			});
			icon.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(item.parentFragment.getActivity(), switch(item.notification.notification.type){
				case FAVORITE -> R.attr.colorFavorite;
				case REBLOG -> R.attr.colorBoost;
				case FOLLOW, FOLLOW_REQUEST, UPDATE -> R.attr.colorM3Primary;
				default -> R.attr.colorM3Outline;
			})));
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.notification.status==null ? V.dp(12) : 0);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.notification.accounts.get((Integer)v.getTag())));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}
	}
}
