package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public class PhotoStatusDisplayItem extends StatusDisplayItem{
	private Attachment attachment;
	private ImageLoaderRequest request;
	public PhotoStatusDisplayItem(Status status, Attachment photo){
		super(status);
		this.attachment=photo;
		request=new UrlImageLoaderRequest(photo.url, 1000, 1000);
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
		private final ImageView photo;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_photo, parent);
			photo=findViewById(R.id.photo);
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
	}
}
