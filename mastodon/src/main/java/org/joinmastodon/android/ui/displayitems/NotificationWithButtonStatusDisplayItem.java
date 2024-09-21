package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.AccountWarning;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.RelationshipSeveranceEvent;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Map;

public class NotificationWithButtonStatusDisplayItem extends StatusDisplayItem{
	private final NotificationViewModel notification;
	private CharSequence text;
	private String buttonText;
	private Runnable buttonAction;

	public NotificationWithButtonStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, NotificationViewModel notification, String accountID){
		super(parentID, parentFragment);
		this.notification=notification;
		String localDomain=AccountSessionManager.get(accountID).domain;
		if(notification.notification.type==NotificationType.SEVERED_RELATIONSHIPS){
			RelationshipSeveranceEvent event=notification.notification.event;
			if(event!=null){
				text=switch(event.type){
					case ACCOUNT_SUSPENSION -> replacePlaceholdersWithBoldStrings(parentFragment.getString(R.string.relationship_severance_account_suspension,
									"{{localDomain}}", "{{target}}"), Map.of("localDomain", localDomain, "target", event.targetName));
					case DOMAIN_BLOCK -> replacePlaceholdersWithBoldStrings(parentFragment.getString(R.string.relationship_severance_domain_block,
									"{{localDomain}}", "{{target}}", event.followersCount, parentFragment.getResources().getQuantityString(R.plurals.x_accounts, event.followingCount, event.followingCount)),
									Map.of("localDomain", localDomain, "target", event.targetName));
					case USER_DOMAIN_BLOCK -> replacePlaceholdersWithBoldStrings(parentFragment.getString(R.string.relationship_severance_user_domain_block,
									"{{target}}", event.followersCount, parentFragment.getResources().getQuantityString(R.plurals.x_accounts, event.followingCount, event.followingCount)),
									Map.of("target", event.targetName));
				};
			}else{
				text="???";
			}
			buttonText=parentFragment.getString(R.string.relationship_severance_learn_more);
			buttonAction=()->UiUtils.launchWebBrowser(parentFragment.getActivity(), "https://"+localDomain+"/severed_relationships");
		}else if(notification.notification.type==NotificationType.MODERATION_WARNING){
			AccountWarning warning=notification.notification.moderationWarning;
			if(warning!=null){
				text=parentFragment.getString(switch(warning.action){
					case NONE -> R.string.moderation_warning_action_none;
					case DISABLE -> R.string.moderation_warning_action_disable;
					case MARK_STATUSES_AS_SENSITIVE -> R.string.moderation_warning_action_mark_statuses_as_sensitive;
					case DELETE_STATUSES -> R.string.moderation_warning_action_delete_statuses;
					case SENSITIVE -> R.string.moderation_warning_action_sensitive;
					case SILENCE -> R.string.moderation_warning_action_silence;
					case SUSPEND -> R.string.moderation_warning_action_suspend;
				});
				buttonAction=()->UiUtils.launchWebBrowser(parentFragment.getActivity(), "https://"+localDomain+"/disputes/strikes/"+warning.id);
			}else{
				text="???";
			}
			buttonText=parentFragment.getString(R.string.moderation_warning_learn_more);
		}
	}

	private SpannableStringBuilder replacePlaceholdersWithBoldStrings(String in, Map<String, String> replacements){
		SpannableStringBuilder ssb=new SpannableStringBuilder(in);
		for(Map.Entry<String, String> e:replacements.entrySet()){
			String placeholder="{{"+e.getKey()+"}}";
			int index=ssb.toString().indexOf(placeholder);
			if(index==-1)
				continue;
			ssb.replace(index, index+placeholder.length(), e.getValue());
			ssb.setSpan(new StyleSpan(Typeface.BOLD), index, index+e.getValue().length(), 0);
		}
		return ssb;
	}

	@Override
	public Type getType(){
		return Type.NOTIFICATION_WITH_BUTTON;
	}

	public static class Holder extends StatusDisplayItem.Holder<NotificationWithButtonStatusDisplayItem>{
		private final ImageView icon;
		private final TextView text;
		private final Button button;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_notification_with_button, parent);
			icon=findViewById(R.id.icon);
			text=findViewById(R.id.text);
			button=findViewById(R.id.button);
			button.setOnClickListener(v->item.buttonAction.run());
		}

		@Override
		public void onBind(NotificationWithButtonStatusDisplayItem item){
			icon.setImageResource(switch(item.notification.notification.type){
				case SEVERED_RELATIONSHIPS -> R.drawable.ic_heart_broken_fill1_24px;
				case MODERATION_WARNING -> R.drawable.ic_gavel_24px;
				default -> throw new IllegalStateException("Unexpected value: " + item.notification.notification.type);
			});
			icon.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Outline)));
			text.setText(item.text);
			button.setText(item.buttonText);
			button.setEnabled(item.buttonAction!=null);
		}

		@Override
		public boolean isEnabled(){
			return false;
		}
	}
}
