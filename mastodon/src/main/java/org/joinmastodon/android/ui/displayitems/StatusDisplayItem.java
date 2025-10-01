package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Quote;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.photoviewer.PhotoViewerHost;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.MediaAttachmentViewController;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.joinmastodon.android.utils.TypedObjectPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class StatusDisplayItem{
	public final String parentID;
	public final Callbacks callbacks;
	public Context context;
	public boolean fullWidth; // aka "highlighted"
	public boolean isQuote;
	public int index;

	public static final int FLAG_FULL_WIDTH=1;
	public static final int FLAG_NO_FOOTER=1 << 1;
	public static final int FLAG_CHECKABLE=1 << 2;
	public static final int FLAG_MEDIA_FORCE_HIDDEN=1 << 3;
	public static final int FLAG_NO_HEADER=1 << 4;
	public static final int FLAG_NO_IN_REPLY_TO=1 << 5;
	public static final int FLAG_IS_QUOTE=1 << 6;

	public StatusDisplayItem(String parentID, Callbacks callbacks, Context context){
		this.parentID=parentID;
		this.callbacks=callbacks;
		this.context=context;
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
			case HEADER_COMPACT -> new CompactHeaderStatusDisplayItem.Holder(activity, parent);
			case REBLOG_OR_REPLY_LINE -> new ReblogOrReplyLineStatusDisplayItem.Holder(activity, parent);
			case TEXT -> new TextStatusDisplayItem.Holder(activity, parent);
			case AUDIO -> new AudioStatusDisplayItem.Holder(activity, parent);
			case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
			case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
			case CARD_LARGE -> new LinkCardStatusDisplayItem.Holder(activity, parent, true, parentFragment.getArguments().getString("account"));
			case CARD_COMPACT -> new LinkCardStatusDisplayItem.Holder(activity, parent, false, parentFragment.getArguments().getString("account"));
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
			case FOLLOW_REQUEST_ACTIONS -> new FollowRequestActionsDisplayItem.Holder(activity, parent);
			case QUOTE_ERROR -> new QuoteErrorStatusDisplayItem.Holder(activity, parent);
			case NESTED_QUOTE -> new NestedQuoteStatusDisplayItem.Holder(activity, parent);
		};
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean addFooter){
		int flags=0;
		if(!addFooter)
			flags|=FLAG_NO_FOOTER;
		return buildItems(fragment, fragment.getActivity(), status, accountID, parentObject, knownAccounts, flags);
	}

	public static ArrayList<StatusDisplayItem> buildItems(Callbacks callbacks, Context context, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, int flags){
		if(callbacks==null)
			callbacks=new NoOpCallbacks(context);
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.getContentStatus();
		StatusDisplayItem header=null;
		boolean hideCounts=!GlobalUserPreferences.showInteractionCounts;
		if((flags & FLAG_NO_HEADER)==0){
			if(status.reblog!=null){
				items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, callbacks, context, context.getString(R.string.user_boosted), status.account, R.drawable.ic_repeat_wght700_20px, accountID));
			}else if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId) && (flags & FLAG_NO_IN_REPLY_TO)==0){
				Account account=Objects.requireNonNull(knownAccounts.get(status.inReplyToAccountId));
				items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, callbacks, context, context.getString(R.string.in_reply_to), account, R.drawable.ic_reply_wght700_20px, accountID));
			}
			if((flags & FLAG_CHECKABLE)!=0)
				items.add(header=new CheckableHeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, callbacks, context, accountID, statusForContent, null));
			else if((flags &FLAG_IS_QUOTE)!=0)
				items.add(header=new CompactHeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, callbacks, context, accountID, statusForContent));
			else
				items.add(header=new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, callbacks, context, accountID, statusForContent, null));
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
			SpoilerStatusDisplayItem spoilerItem=new SpoilerStatusDisplayItem(parentID, callbacks, context, context.getString(R.string.post_matches_filter_x, status.filtered.get(0).filter.title), status, statusForContent, Type.FILTER_SPOILER, Status.SpoilerType.FILTER);
			contentItems.add(spoilerItem);
			contentItems=spoilerItem.contentItems;
			cwParentItems=contentItems;
		}
		if(!TextUtils.isEmpty(statusForContent.spoilerText)){
			SpoilerStatusDisplayItem spoilerItem=new SpoilerStatusDisplayItem(parentID, callbacks, context, null, status, statusForContent, Type.SPOILER, Status.SpoilerType.CONTENT_WARNING);
			contentItems.add(spoilerItem);
			contentItems=spoilerItem.contentItems;
			if(!GlobalUserPreferences.showCWs && !filtered){
				status.revealedSpoilers.add(Status.SpoilerType.CONTENT_WARNING);
			}
			needAddCWItems=status.revealedSpoilers.contains(Status.SpoilerType.CONTENT_WARNING);
		}

		if(!TextUtils.isEmpty(statusForContent.content)){
			SpannableStringBuilder parsedText=HtmlParser.parse(statusForContent.content, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID, statusForContent, context);
			if(filtered){
				HtmlParser.applyFilterHighlights(context, parsedText, status.filtered);
			}
			TextStatusDisplayItem text=new TextStatusDisplayItem(parentID, parsedText, callbacks, context, statusForContent, accountID);
			contentItems.add(text);
		}else if(header instanceof HeaderStatusDisplayItem hsdi){
			hsdi.needBottomPadding=true;
		}

		List<Attachment> imageAttachments=statusForContent.mediaAttachments.stream().filter(att->att.type.isImage()).collect(Collectors.toList());
		if(!imageAttachments.isEmpty()){
			PhotoLayoutHelper.TiledLayoutResult layout=PhotoLayoutHelper.processThumbs(imageAttachments);
			MediaGridStatusDisplayItem mediaGrid=new MediaGridStatusDisplayItem(parentID, callbacks, context, layout, imageAttachments, statusForContent);
			if((flags & FLAG_MEDIA_FORCE_HIDDEN)!=0){
				mediaGrid.sensitiveTitle=context.getString(R.string.media_hidden);
				mediaGrid.sensitiveRevealed=false;
			}else if(statusForContent.sensitive && !GlobalUserPreferences.hideSensitiveMedia){
				mediaGrid.sensitiveRevealed=true;
			}
			contentItems.add(mediaGrid);
		}
		for(Attachment att:statusForContent.mediaAttachments){
			if(att.type==Attachment.Type.AUDIO){
				contentItems.add(new AudioStatusDisplayItem(parentID, callbacks, context, statusForContent, att));
			}
		}
		if(statusForContent.poll!=null){
			buildPollItems(parentID, callbacks, context, statusForContent.poll, status, contentItems);
		}
		if(statusForContent.card!=null && statusForContent.mediaAttachments.isEmpty() && TextUtils.isEmpty(statusForContent.spoilerText)){
			contentItems.add(new LinkCardStatusDisplayItem(parentID, callbacks, context, statusForContent, accountID));
		}
		if(statusForContent.quote!=null){
			if(statusForContent.quote.state==Quote.State.ACCEPTED){
				if(statusForContent.quote.quotedStatus!=null && (flags & FLAG_IS_QUOTE)==0){
					ArrayList<StatusDisplayItem> quoteItems=buildItems(callbacks, context, statusForContent.quote.quotedStatus, accountID, parentObject, knownAccounts, FLAG_NO_FOOTER | FLAG_NO_IN_REPLY_TO |FLAG_IS_QUOTE);
					for(StatusDisplayItem item:quoteItems){
						item.isQuote=true;
						if(item instanceof SpoilerStatusDisplayItem spoiler){
							for(StatusDisplayItem subItem:spoiler.contentItems){
								subItem.isQuote=true;
							}
						}
					}
					contentItems.addAll(quoteItems);
				}else if((flags & FLAG_IS_QUOTE)!=0){
					String statusID;
					if(statusForContent.quote.quotedStatus!=null)
						statusID=statusForContent.quote.quotedStatus.id;
					else
						statusID=statusForContent.quote.quotedStatusId;
					contentItems.add(new NestedQuoteStatusDisplayItem(parentID, callbacks, context, statusID, statusForContent.quote));
				}
			}else if((flags & FLAG_IS_QUOTE)!=0){
				contentItems.add(new NestedQuoteStatusDisplayItem(parentID, callbacks, context, null, statusForContent.quote));
			}else{
				contentItems.add(new QuoteErrorStatusDisplayItem(parentID, callbacks, context, statusForContent.quote.state));
			}
		}
		if(needAddCWItems){
			cwParentItems.addAll(contentItems);
		}
		if((flags & FLAG_NO_FOOTER)==0){
			FooterStatusDisplayItem footer=new FooterStatusDisplayItem(parentID, callbacks, context, statusForContent, accountID);
			footer.hideCounts=hideCounts;
			items.add(footer);
			if(status.hasGapAfter && !(callbacks instanceof ThreadFragment))
				items.add(new GapStatusDisplayItem(parentID, callbacks, context));
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

	public static void buildPollItems(String parentID, Callbacks callbacks, Context context, Poll poll, Status status, List<StatusDisplayItem> items){
		int i=0;
		for(Poll.Option opt:poll.options){
			items.add(new PollOptionStatusDisplayItem(parentID, poll, i, callbacks, context, status));
			i++;
		}
		items.add(new PollFooterStatusDisplayItem(parentID, callbacks, context, poll, status));
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
		NOTIFICATION_WITH_BUTTON,
		FOLLOW_REQUEST_ACTIONS,
		HEADER_COMPACT,
		QUOTE_ERROR,
		NESTED_QUOTE
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
				sdi.callbacks.onItemClick(sdi.parentID);
		}

		@Override
		public void onClick(float x, float y){
			if(item instanceof StatusDisplayItem sdi && sdi.isQuote){
				int quoteLeft, quoteRight;
				if(sdi.fullWidth){
					quoteLeft=V.dp(16);
					quoteRight=itemView.getWidth()-V.dp(16);
				}else if(itemView.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL){
					quoteLeft=V.dp(16);
					quoteRight=itemView.getWidth()-V.dp(48+16);
				}else{
					quoteLeft=V.dp(48+16);
					quoteRight=itemView.getWidth()-V.dp(16);
				}
				if(x<quoteRight && x>quoteLeft){
					sdi.callbacks.onItemClick(sdi.parentID, true);
					return;
				}
			}
			UsableRecyclerView.DisableableClickable.super.onClick(x, y);
		}

		@Override
		public boolean isEnabled(){
			return item instanceof StatusDisplayItem sdi && sdi.callbacks.isItemEnabled(sdi);
		}
	}

	public interface Callbacks extends PhotoViewerHost{
		void onItemClick(String parentID);
		void onItemClick(String parentID, boolean quote);
		boolean isItemEnabled(StatusDisplayItem item);
		TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> getAttachmentViewsPool();
		void retryFailedImages();
		void onPollOptionClick(PollOptionStatusDisplayItem.Holder holder);
		void onPollToggleResultsClick(PollFooterStatusDisplayItem.Holder holder);
		void onPollVoteButtonClick(PollFooterStatusDisplayItem.Holder holder);
		void onRevealSpoilerClick(SpoilerStatusDisplayItem.Holder holder);
		void onGapClick(GapStatusDisplayItem.Holder item);
		Relationship getRelationship(String id);
		void putRelationship(String id, Relationship rel);
		void togglePostTranslation(Status status, String itemID);
		void maybeShowPreReplySheet(Status status, Runnable proceed);
	}

	public static class NoOpCallbacks implements Callbacks{
		private Context context;
		private TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> attachmentViewsPool=new TypedObjectPool<>(type->new MediaAttachmentViewController(context, type));

		public NoOpCallbacks(Context context){
			this.context=context;
		}

		@Override
		public void onItemClick(String parentID){

		}

		@Override
		public void onItemClick(String parentID, boolean quote){

		}

		@Override
		public boolean isItemEnabled(StatusDisplayItem item){
			return false;
		}

		@Override
		public TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> getAttachmentViewsPool(){
			return attachmentViewsPool;
		}

		@Override
		public void retryFailedImages(){

		}

		@Override
		public void onPollOptionClick(PollOptionStatusDisplayItem.Holder holder){

		}

		@Override
		public void onPollToggleResultsClick(PollFooterStatusDisplayItem.Holder holder){

		}

		@Override
		public void onPollVoteButtonClick(PollFooterStatusDisplayItem.Holder holder){

		}

		@Override
		public void onRevealSpoilerClick(SpoilerStatusDisplayItem.Holder holder){

		}

		@Override
		public void onGapClick(GapStatusDisplayItem.Holder item){

		}

		@Override
		public Relationship getRelationship(String id){
			return null;
		}

		@Override
		public void putRelationship(String id, Relationship rel){

		}

		@Override
		public void togglePostTranslation(Status status, String itemID){

		}

		@Override
		public void maybeShowPreReplySheet(Status status, Runnable proceed){

		}

		@Override
		public void openPhotoViewer(String parentID, Status status, int attachmentIndex, MediaGridStatusDisplayItem.Holder gridHolder){

		}
	}
}
