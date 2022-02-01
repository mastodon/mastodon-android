package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;

import java.util.ArrayList;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public abstract class StatusDisplayItem{
	public final String parentID;

	public StatusDisplayItem(String parentID){
		this.parentID=parentID;
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
			case FOOTER -> new FooterStatusDisplayItem.Holder(activity, parent);
			default -> throw new UnsupportedOperationException();
		};
	}

	public static ArrayList<StatusDisplayItem> buildItems(Fragment fragment, Status status, String accountID, DisplayItemsParent parentObject){
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.reblog==null ? status : status.reblog;
		if(status.reblog!=null){
			items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment.getString(R.string.user_boosted, status.account.displayName)));
		}
		items.add(new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID));
		if(!TextUtils.isEmpty(statusForContent.content))
			items.add(new TextStatusDisplayItem(parentID, HtmlParser.parse(statusForContent.content, statusForContent.emojis), fragment));
		for(Attachment attachment:statusForContent.mediaAttachments){
			if(attachment.type==Attachment.Type.IMAGE){
				items.add(new PhotoStatusDisplayItem(parentID, status, attachment, fragment));
			}
		}
		items.add(new FooterStatusDisplayItem(parentID, status, accountID));
		return items;
	}

	public enum Type{
		HEADER,
		REBLOG_OR_REPLY_LINE,
		TEXT,
		PHOTO,
		VIDEO,
		GIFV,
		AUDIO,
		POLL_HEADER,
		POLL_OPTION,
		POLL_FOOTER,
		CARD,
		FOOTER,
	}
}
