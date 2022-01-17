package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;

import java.time.Instant;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;

	public HeaderStatusDisplayItem(Status status, Account user, Instant createdAt){
		super(status);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(user.avatar);
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return avaRequest;
	}

	public static class Holder extends BindableViewHolder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, subtitle;
		private final ImageView avatar;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header, parent);
			name=findViewById(R.id.name);
			subtitle=findViewById(R.id.subtitle);
			avatar=findViewById(R.id.avatar);
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.user.displayName);
			subtitle.setText('@'+item.user.acct);
		}

		@Override
		public void setImage(int index, Drawable drawable){
			avatar.setImageDrawable(drawable);
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			avatar.setImageBitmap(null);
		}
	}
}
