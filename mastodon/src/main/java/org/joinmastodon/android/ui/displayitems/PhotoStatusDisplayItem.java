package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.ViewGroup;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;

import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;

public class PhotoStatusDisplayItem extends ImageStatusDisplayItem{
	public PhotoStatusDisplayItem(String parentID, Status status, Attachment photo, BaseStatusListFragment parentFragment, int index, int totalPhotos){
		super(parentID, parentFragment, photo, status, index, totalPhotos);
		request=new UrlImageLoaderRequest(photo.url, 1000, 1000);
	}

	@Override
	public Type getType(){
		return Type.PHOTO;
	}

	public static class Holder extends ImageStatusDisplayItem.Holder<PhotoStatusDisplayItem>{

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_photo, parent);
		}
	}
}
