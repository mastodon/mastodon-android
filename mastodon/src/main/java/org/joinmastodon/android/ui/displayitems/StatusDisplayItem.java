package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.text.HtmlParser;

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
	public final BaseStatusListFragment parentFragment;

	public StatusDisplayItem(String parentID, BaseStatusListFragment parentFragment){
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

	public static BindableViewHolder<? extends StatusDisplayItem> createViewHolder(Type type, Activity activity, ViewGroup parent){
		return switch(type){
			case HEADER -> new HeaderStatusDisplayItem.Holder(activity, parent);
			case REBLOG_OR_REPLY_LINE -> new ReblogOrReplyLineStatusDisplayItem.Holder(activity, parent);
			case TEXT -> new TextStatusDisplayItem.Holder(activity, parent);
			case PHOTO -> new PhotoStatusDisplayItem.Holder(activity, parent);
			case GIFV -> new GifVStatusDisplayItem.Holder(activity, parent);
			case VIDEO -> new VideoStatusDisplayItem.Holder(activity, parent);
			case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
			case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
			case FOOTER -> new FooterStatusDisplayItem.Holder(activity, parent);
			default -> throw new UnsupportedOperationException();
		};
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts){
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.getContentStatus();
		if(status.reblog!=null){
			items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, fragment.getString(R.string.user_boosted, status.account.displayName), status.account.emojis, R.drawable.ic_fluent_arrow_repeat_all_20_filled));
		}else if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId)){
			Account account=Objects.requireNonNull(knownAccounts.get(status.inReplyToAccountId));
			items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, fragment.getString(R.string.in_reply_to, account.displayName), account.emojis, R.drawable.ic_fluent_arrow_reply_20_filled));
		}
		HeaderStatusDisplayItem header;
		items.add(header=new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent));
		if(!TextUtils.isEmpty(statusForContent.content))
			items.add(new TextStatusDisplayItem(parentID, HtmlParser.parse(statusForContent.content, statusForContent.emojis, statusForContent.mentions, accountID), fragment, statusForContent));
		else
			header.needBottomPadding=true;
		List<Attachment> imageAttachments=statusForContent.mediaAttachments.stream().filter(att->att.type.isImage()).collect(Collectors.toList());
		if(!imageAttachments.isEmpty()){
			int photoIndex=0;
			PhotoLayoutHelper.TiledLayoutResult layout=PhotoLayoutHelper.processThumbs(1000, 1910, imageAttachments);
			for(Attachment attachment:imageAttachments){
				if(attachment.type==Attachment.Type.IMAGE){
					items.add(new PhotoStatusDisplayItem(parentID, statusForContent, attachment, fragment, photoIndex, imageAttachments.size(), layout, layout.tiles[photoIndex]));
				}else if(attachment.type==Attachment.Type.GIFV){
					items.add(new GifVStatusDisplayItem(parentID, statusForContent, attachment, fragment, photoIndex, imageAttachments.size(), layout, layout.tiles[photoIndex]));
				}else if(attachment.type==Attachment.Type.VIDEO){
					items.add(new VideoStatusDisplayItem(parentID, statusForContent, attachment, fragment, photoIndex, imageAttachments.size(), layout, layout.tiles[photoIndex]));
				}else{
					throw new IllegalStateException("This isn't supposed to happen, type is "+attachment.type);
				}
				photoIndex++;
			}
		}
		if(statusForContent.poll!=null){
			buildPollItems(parentID, fragment, statusForContent.poll, items);
		}
		items.add(new FooterStatusDisplayItem(parentID, fragment, statusForContent, accountID));
		return items;
	}

	public static void buildPollItems(String parentID, BaseStatusListFragment fragment, Poll poll, List<StatusDisplayItem> items){
		for(Poll.Option opt:poll.options){
			items.add(new PollOptionStatusDisplayItem(parentID, poll, opt, fragment));
		}
		items.add(new PollFooterStatusDisplayItem(parentID, fragment, poll));
	}

	public enum Type{
		HEADER,
		REBLOG_OR_REPLY_LINE,
		TEXT,
		PHOTO,
		VIDEO,
		GIFV,
		AUDIO,
		POLL_OPTION,
		POLL_FOOTER,
		CARD,
		FOOTER,
	}

	public static abstract class Holder<T extends StatusDisplayItem> extends BindableViewHolder<T> implements UsableRecyclerView.Clickable{
		public Holder(View itemView){
			super(itemView);
		}

		public Holder(Context context, int layout, ViewGroup parent){
			super(context, layout, parent);
		}

		public String getItemID(){
			return item.parentID;
		}

		@Override
		public void onClick(){
			item.parentFragment.onItemClick(item.parentID);
		}
	}
}
