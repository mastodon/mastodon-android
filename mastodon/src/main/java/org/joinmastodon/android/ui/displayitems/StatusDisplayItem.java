package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class StatusDisplayItem{
	public final String parentID;
	public final BaseStatusListFragment<?> parentFragment;
	public boolean fullWidth; // aka "highlighted"
	public int index;

	public static final int FLAG_FULL_WIDTH=1;
	public static final int FLAG_NO_FOOTER=1 << 1;
	public static final int FLAG_CHECKABLE=1 << 2;
	public static final int FLAG_MEDIA_FORCE_HIDDEN=1 << 3;
	public static final int FLAG_NO_HEADER=1 << 4;
	public static final int FLAG_NO_IN_REPLY_TO=1 << 5;

	public StatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment){
		this.parentID=parentID;
		this.parentFragment=parentFragment;
	}

	public abstract Type getType();

	public int getImageCount(){
		return 0;
	}

	public ImageLoaderRequest getImageRequest(int index){
		return null;
	}

	public static BindableViewHolder<? extends StatusDisplayItem> createViewHolder(Type type, Activity activity, ViewGroup parent, Fragment parentFragment){
		return switch(type){
			case HEADER -> new HeaderStatusDisplayItem.Holder(activity, parent);
			case HEADER_CHECKABLE -> new CheckableHeaderStatusDisplayItem.Holder(activity, parent);
			case REBLOG_OR_REPLY_LINE -> new ReblogOrReplyLineStatusDisplayItem.Holder(activity, parent);
			case TEXT -> new TextStatusDisplayItem.Holder(activity, parent);
			case AUDIO -> new AudioStatusDisplayItem.Holder(activity, parent);
			case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
			case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
			case CARD_LARGE -> new LinkCardStatusDisplayItem.Holder(activity, parent, true, ((BaseStatusListFragment<?>)parentFragment).getAccountID());
			case CARD_COMPACT -> new LinkCardStatusDisplayItem.Holder(activity, parent, false, ((BaseStatusListFragment<?>)parentFragment).getAccountID());
			case FOOTER -> new FooterStatusDisplayItem.Holder(activity, parent);
			case ACCOUNT -> new AccountStatusDisplayItem.Holder(new AccountViewHolder(parentFragment, parent, null));
			case HASHTAG -> new HashtagStatusDisplayItem.Holder(activity, parent);
			case GAP -> new GapStatusDisplayItem.Holder(activity, parent);
			case EXTENDED_FOOTER -> new ExtendedFooterStatusDisplayItem.Holder(activity, parent);
			case MEDIA_GRID -> new MediaGridStatusDisplayItem.Holder(activity, parent);
			case SPOILER, FILTER_SPOILER -> new SpoilerStatusDisplayItem.Holder(activity, parent, type);
			case SECTION_HEADER -> new SectionHeaderStatusDisplayItem.Holder(activity, parent);
			case NOTIFICATION_HEADER -> new NotificationHeaderStatusDisplayItem.Holder(activity, parent);
			case INLINE_STATUS -> new InlineStatusStatusDisplayItem.Holder(activity, parent);
			case NOTIFICATION_WITH_BUTTON -> new NotificationWithButtonStatusDisplayItem.Holder(activity, parent);
		};
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean addFooter){
		int flags=0;
		if(!addFooter)
			flags|=FLAG_NO_FOOTER;
		return buildItems(fragment, status, accountID, parentObject, knownAccounts, flags);
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, int flags){
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.getContentStatus();
		HeaderStatusDisplayItem header=null;
		boolean hideCounts=!AccountSessionManager.get(accountID).getLocalPreferences().showInteractionCounts;
		if((flags & FLAG_NO_HEADER)==0){
			if(status.reblog!=null){
				items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, fragment.getString(R.string.user_boosted, status.account.displayName), status.account.emojis, R.drawable.ic_repeat_wght700_20px));
			}else if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId) && (flags & FLAG_NO_IN_REPLY_TO)==0){
				Account account=Objects.requireNonNull(knownAccounts.get(status.inReplyToAccountId));
				items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, fragment.getString(R.string.in_reply_to, account.displayName), account.emojis, R.drawable.ic_reply_wght700_20px));
			}
			if((flags & FLAG_CHECKABLE)!=0)
				items.add(header=new CheckableHeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent, null));
			else
				items.add(header=new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent, null));
		}

		boolean filtered=false;
		if(status.filtered!=null){
			for(FilterResult filter:status.filtered){
				if(filter.filter.isActive()){
					filtered=true;
					break;
				}
			}
		}

		ArrayList<StatusDisplayItem> contentItems=items;
		ArrayList<StatusDisplayItem> cwParentItems=items;
		boolean needAddCWItems=false;
		if(filtered){
			SpoilerStatusDisplayItem spoilerItem=new SpoilerStatusDisplayItem(parentID, fragment, fragment.getString(R.string.post_matches_filter_x, status.filtered.get(0).filter.title), status, statusForContent, Type.FILTER_SPOILER, Status.SpoilerType.FILTER);
			contentItems.add(spoilerItem);
			contentItems=spoilerItem.contentItems;
			cwParentItems=contentItems;
		}
		if(!TextUtils.isEmpty(statusForContent.spoilerText)){
			SpoilerStatusDisplayItem spoilerItem=new SpoilerStatusDisplayItem(parentID, fragment, null, status, statusForContent, Type.SPOILER, Status.SpoilerType.CONTENT_WARNING);
			contentItems.add(spoilerItem);
			contentItems=spoilerItem.contentItems;
			if(!AccountSessionManager.get(accountID).getLocalPreferences().showCWs && !filtered){
				status.revealedSpoilers.add(Status.SpoilerType.CONTENT_WARNING);
				needAddCWItems=true;
			}
		}

		if(!TextUtils.isEmpty(statusForContent.content)){
			SpannableStringBuilder parsedText=HtmlParser.parse(statusForContent.content, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID, statusForContent, fragment.getActivity());
			if(filtered){
				HtmlParser.applyFilterHighlights(fragment.getActivity(), parsedText, status.filtered);
			}
			TextStatusDisplayItem text=new TextStatusDisplayItem(parentID, parsedText, fragment, statusForContent);
			contentItems.add(text);
		}else if(header!=null){
			header.needBottomPadding=true;
		}

		List<Attachment> imageAttachments=statusForContent.mediaAttachments.stream().filter(att->att.type.isImage()).collect(Collectors.toList());
		if(!imageAttachments.isEmpty()){
			PhotoLayoutHelper.TiledLayoutResult layout=PhotoLayoutHelper.processThumbs(imageAttachments);
			MediaGridStatusDisplayItem mediaGrid=new MediaGridStatusDisplayItem(parentID, fragment, layout, imageAttachments, statusForContent);
			if((flags & FLAG_MEDIA_FORCE_HIDDEN)!=0){
				mediaGrid.sensitiveTitle=fragment.getString(R.string.media_hidden);
				mediaGrid.sensitiveRevealed=false;
			}else if(statusForContent.sensitive && !AccountSessionManager.get(accountID).getLocalPreferences().hideSensitiveMedia){
				mediaGrid.sensitiveRevealed=true;
			}
			contentItems.add(mediaGrid);
		}
		for(Attachment att:statusForContent.mediaAttachments){
			if(att.type==Attachment.Type.AUDIO){
				contentItems.add(new AudioStatusDisplayItem(parentID, fragment, statusForContent, att));
			}
		}
		if(statusForContent.poll!=null){
			buildPollItems(parentID, fragment, statusForContent.poll, status, contentItems);
		}
		if(statusForContent.card!=null && statusForContent.mediaAttachments.isEmpty() && TextUtils.isEmpty(statusForContent.spoilerText)){
			contentItems.add(new LinkCardStatusDisplayItem(parentID, fragment, statusForContent));
		}
		if(needAddCWItems){
			cwParentItems.addAll(contentItems);
		}
		if((flags & FLAG_NO_FOOTER)==0){
			FooterStatusDisplayItem footer=new FooterStatusDisplayItem(parentID, fragment, statusForContent, accountID);
			footer.hideCounts=hideCounts;
			items.add(footer);
			if(status.hasGapAfter && !(fragment instanceof ThreadFragment))
				items.add(new GapStatusDisplayItem(parentID, fragment));
		}
		int i=1;
		boolean fullWidth=(flags & FLAG_FULL_WIDTH)!=0;
		for(StatusDisplayItem item:items){
			item.fullWidth=fullWidth;
			item.index=i++;
		}
		if(items!=contentItems){
			for(StatusDisplayItem item:contentItems){
				item.index=i++;
			}
		}
		return items;
	}

	public static void buildPollItems(String parentID, BaseStatusListFragment<?> fragment, Poll poll, Status status, List<StatusDisplayItem> items){
		int i=0;
		for(Poll.Option opt:poll.options){
			items.add(new PollOptionStatusDisplayItem(parentID, poll, i, fragment, status));
			i++;
		}
		items.add(new PollFooterStatusDisplayItem(parentID, fragment, poll, status));
	}

	public enum Type{
		HEADER,
		REBLOG_OR_REPLY_LINE,
		TEXT,
		AUDIO,
		POLL_OPTION,
		POLL_FOOTER,
		CARD_LARGE,
		CARD_COMPACT,
		FOOTER,
		ACCOUNT,
		HASHTAG,
		GAP,
		EXTENDED_FOOTER,
		MEDIA_GRID,
		SPOILER,
		SECTION_HEADER,
		HEADER_CHECKABLE,
		NOTIFICATION_HEADER,
		FILTER_SPOILER,
		INLINE_STATUS,
		NOTIFICATION_WITH_BUTTON
	}

	public static abstract class Holder<T> extends BindableViewHolder<T> implements UsableRecyclerView.DisableableClickable{
		public Holder(View itemView){
			super(itemView);
		}

		public Holder(Context context, int layout, ViewGroup parent){
			super(context, layout, parent);
		}

		public String getItemID(){
			return item instanceof StatusDisplayItem sdi ? sdi.parentID : null;
		}

		@Override
		public void onClick(){
			if(item instanceof StatusDisplayItem sdi)
				sdi.parentFragment.onItemClick(sdi.parentID);
		}

		@Override
		public boolean isEnabled(){
			return item instanceof StatusDisplayItem sdi && sdi.parentFragment.isItemEnabled(sdi.parentID);
		}
	}
}
