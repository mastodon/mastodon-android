package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;

import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;

public class GifVStatusDisplayItem extends ImageStatusDisplayItem{
	public GifVStatusDisplayItem(String parentID, Status status, Attachment attachment, BaseStatusListFragment parentFragment, int index, int totalPhotos, PhotoLayoutHelper.TiledLayoutResult tiledLayout, PhotoLayoutHelper.TiledLayoutResult.Tile thisTile){
		super(parentID, parentFragment, attachment, status, index, totalPhotos, tiledLayout, thisTile);
		request=new UrlImageLoaderRequest(attachment.previewUrl, 1000, 1000);
	}

	@Override
	public Type getType(){
		return Type.GIFV;
	}

	public static class Holder extends ImageStatusDisplayItem.Holder<GifVStatusDisplayItem>{

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_gifv, parent);
			View play=findViewById(R.id.play_button);
			play.setOutlineProvider(new ViewOutlineProvider(){
				@Override
				public void getOutline(View view, Outline outline){
					outline.setOval(0, 0, view.getWidth(), view.getHeight());
					outline.setAlpha(.99f); // fixes shadow rendering
				}
			});
		}
	}
}
