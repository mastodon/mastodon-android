package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.ViewGroup;

import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public abstract class StatusDisplayItem{
	public final Status status;

	public StatusDisplayItem(Status status){
		this.status=status;
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
			default -> throw new UnsupportedOperationException();
		};
	}

	public static List<StatusDisplayItem> buildItems(Fragment fragment, Status status){
		ArrayList<StatusDisplayItem> items=new ArrayList<>();
		Status statusForContent=status.reblog==null ? status : status.reblog;
		if(status.reblog!=null){
			items.add(new ReblogOrReplyLineStatusDisplayItem(status));
		}
		items.add(new HeaderStatusDisplayItem(status, statusForContent.account, statusForContent.createdAt));
		if(!TextUtils.isEmpty(statusForContent.content))
			items.add(new TextStatusDisplayItem(status, statusForContent.processedContent, fragment));
		for(Attachment attachment:statusForContent.mediaAttachments){
			if(attachment.type==Attachment.Type.IMAGE){
				items.add(new PhotoStatusDisplayItem(status, attachment));
			}
		}
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
