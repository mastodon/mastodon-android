package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.photoviewer.PhotoViewerHost;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public class PhotoStatusDisplayItem extends StatusDisplayItem{
	private Attachment attachment;
	private ImageLoaderRequest request;
	private Fragment parentFragment;
	private Status status;
	public PhotoStatusDisplayItem(String parentID, Status status, Attachment photo, Fragment parentFragment){
		super(parentID);
		this.status=status;
		this.attachment=photo;
		request=new UrlImageLoaderRequest(photo.url, 1000, 1000);
		this.parentFragment=parentFragment;
	}

	@Override
	public Type getType(){
		return Type.PHOTO;
	}

	@Override
	public int getImageCount(){
		return 1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return request;
	}

	public static class Holder extends BindableViewHolder<PhotoStatusDisplayItem> implements ImageLoaderViewHolder{
		public final ImageView photo;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_photo, parent);
			photo=findViewById(R.id.photo);
			photo.setOnClickListener(this::onViewClick);
		}

		@Override
		public void onBind(PhotoStatusDisplayItem item){

		}

		@Override
		public void setImage(int index, Drawable drawable){
			photo.setImageDrawable(drawable);
		}

		@Override
		public void clearImage(int index){
			photo.setImageDrawable(item.attachment.blurhashPlaceholder);
		}

		private void onViewClick(View v){
			if(item.parentFragment instanceof PhotoViewerHost){
				Status contentStatus=item.status.reblog!=null ? item.status.reblog : item.status;
				((PhotoViewerHost) item.parentFragment).openPhotoViewer(item.parentID, item.status, contentStatus.mediaAttachments.indexOf(item.attachment));
			}
		}
	}
}
