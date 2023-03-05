package org.joinmastodon.android.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;

public class MediaAttachmentViewController{
	public final View view;
	public final MediaGridStatusDisplayItem.GridItemType type;
	public final ImageView photo;
	public final View altButton;
	private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
	private final Context context;
	private boolean didClear;
	private Status status;

	public MediaAttachmentViewController(Context context, MediaGridStatusDisplayItem.GridItemType type){
		view=context.getSystemService(LayoutInflater.class).inflate(switch(type){
				case PHOTO -> R.layout.display_item_photo;
				case VIDEO -> R.layout.display_item_video;
				case GIFV -> R.layout.display_item_gifv;
			}, null);
		photo=view.findViewById(R.id.photo);
		altButton=view.findViewById(R.id.alt_button);
		this.type=type;
		this.context=context;
	}

	public void bind(Attachment attachment, Status status){
		this.status=status;
		crossfadeDrawable.setSize(attachment.getWidth(), attachment.getHeight());
		crossfadeDrawable.setBlurhashDrawable(attachment.blurhashPlaceholder);
		crossfadeDrawable.setCrossfadeAlpha(status.spoilerRevealed ? 0f : 1f);
		photo.setImageDrawable(null);
		photo.setImageDrawable(crossfadeDrawable);
		photo.setContentDescription(TextUtils.isEmpty(attachment.description) ? context.getString(R.string.media_no_description) : attachment.description);
		if(altButton!=null){
			altButton.setVisibility(TextUtils.isEmpty(attachment.description) ? View.GONE : View.VISIBLE);
		}
		didClear=false;
	}

	public void setImage(Drawable drawable){
		crossfadeDrawable.setImageDrawable(drawable);
		if(didClear && status.spoilerRevealed)
			 crossfadeDrawable.animateAlpha(0f);
	}

	public void clearImage(){
		crossfadeDrawable.setCrossfadeAlpha(1f);
		crossfadeDrawable.setImageDrawable(null);
		didClear=true;
	}

	public void setRevealed(boolean revealed){
		crossfadeDrawable.animateAlpha(revealed ? 0f : 1f);
	}
}
