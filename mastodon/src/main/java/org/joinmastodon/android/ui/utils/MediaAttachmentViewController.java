package org.joinmastodon.android.ui.utils;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;
import org.joinmastodon.android.ui.drawables.PlayIconDrawable;
import org.joinmastodon.android.ui.views.MediaGridLayout;

import me.grishka.appkit.utils.V;

public class MediaAttachmentViewController{
	public final View view;
	public final MediaGridStatusDisplayItem.GridItemType type;
	public final ImageView photo;
	public final View altButton;
	public final TextView duration;
	public final View playButton;
	public final View failedOverlay;
	public final View failedText;
	private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
	private final Context context;
	private boolean didClear;
	private Status status;
	private Attachment attachment;
	private ViewOutlineProvider outlineProvider=new ViewOutlineProvider(){
		@Override
		public void getOutline(View view, Outline outline){
			MediaGridLayout.LayoutParams lp=(MediaGridLayout.LayoutParams) MediaAttachmentViewController.this.view.getLayoutParams();
			int mask=lp.tile.getRoundCornersMask();
			int radius=V.dp(8);
			if(mask==(PhotoLayoutHelper.CORNER_TL | PhotoLayoutHelper.CORNER_TR | PhotoLayoutHelper.CORNER_BL | PhotoLayoutHelper.CORNER_BR)){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
			}else if(mask==(PhotoLayoutHelper.CORNER_TL | PhotoLayoutHelper.CORNER_TR)){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight()+radius, radius);
			}else if(mask==(PhotoLayoutHelper.CORNER_BL | PhotoLayoutHelper.CORNER_BR)){
				outline.setRoundRect(0, -radius, view.getWidth(), view.getHeight(), radius);
			}else if(mask==(PhotoLayoutHelper.CORNER_TL | PhotoLayoutHelper.CORNER_BL)){
				outline.setRoundRect(0, 0, view.getWidth()+radius, view.getHeight(), radius);
			}else if(mask==(PhotoLayoutHelper.CORNER_TR | PhotoLayoutHelper.CORNER_BR)){
				outline.setRoundRect(-radius, 0, view.getWidth(), view.getHeight(), radius);
			}else if(mask==PhotoLayoutHelper.CORNER_TL){
				outline.setRoundRect(0, 0, view.getWidth()+radius, view.getHeight()+radius, radius);
			}else if(mask==PhotoLayoutHelper.CORNER_TR){
				outline.setRoundRect(-radius, 0, view.getWidth(), view.getHeight()+radius, radius);
			}else if(mask==PhotoLayoutHelper.CORNER_BL){
				outline.setRoundRect(0, -radius, view.getWidth()+radius, view.getHeight(), radius);
			}else if(mask==PhotoLayoutHelper.CORNER_BR){
				outline.setRoundRect(-radius, -radius, view.getWidth(), view.getHeight(), radius);
			}else{
				outline.setRect(0, 0, view.getWidth(), view.getHeight());
			}
		}
	};

	public MediaAttachmentViewController(Context context, MediaGridStatusDisplayItem.GridItemType type){
		view=context.getSystemService(LayoutInflater.class).inflate(switch(type){
				case PHOTO -> R.layout.display_item_photo;
				case VIDEO -> R.layout.display_item_video;
				case GIFV -> R.layout.display_item_gifv;
			}, null);
		photo=view.findViewById(R.id.photo);
		altButton=view.findViewById(R.id.alt_button);
		duration=view.findViewById(R.id.duration);
		playButton=view.findViewById(R.id.play_button);
		failedOverlay=view.findViewById(R.id.failed_overlay);
		failedText=view.findViewById(R.id.failed_text);
		this.type=type;
		this.context=context;
		if(playButton!=null){
			// https://developer.android.com/topic/performance/hardware-accel#drawing-support
			if(Build.VERSION.SDK_INT<28)
				playButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			playButton.setBackground(new PlayIconDrawable(context));
		}
		photo.setOutlineProvider(outlineProvider);
		photo.setClipToOutline(true);
		if(failedOverlay!=null){
			failedOverlay.setOutlineProvider(outlineProvider);
			failedOverlay.setClipToOutline(true);
		}
	}

	public void bind(Attachment attachment, Status status){
		this.status=status;
		this.attachment=attachment;
		crossfadeDrawable.setSize(attachment.getWidth(), attachment.getHeight());
		crossfadeDrawable.setBlurhashDrawable(attachment.blurhashPlaceholder);
		crossfadeDrawable.setCrossfadeAlpha(0f);
		photo.setImageDrawable(null);
		photo.setImageDrawable(crossfadeDrawable);
		photo.setContentDescription(TextUtils.isEmpty(attachment.description) ? context.getString(R.string.media_no_description) : attachment.description);
		if(altButton!=null){
			altButton.setVisibility(TextUtils.isEmpty(attachment.description) ? View.GONE : View.VISIBLE);
		}
		if(type==MediaGridStatusDisplayItem.GridItemType.VIDEO){
			duration.setText(UiUtils.formatMediaDuration((int)attachment.getDuration()));
		}
		didClear=false;
		if(failedOverlay!=null){
			V.cancelVisibilityAnimation(failedOverlay);
			failedOverlay.setVisibility(View.GONE);
			failedText.setVisibility(status.mediaAttachments.size()>1 ? View.GONE : View.VISIBLE);
		}
	}

	public void setImage(Drawable drawable){
		crossfadeDrawable.setImageDrawable(drawable);
		if(didClear)
			 crossfadeDrawable.animateAlpha(0f);
		// Make sure the image is not stretched if the server returned wrong dimensions
		if(drawable!=null && (drawable.getIntrinsicWidth()!=attachment.getWidth() || drawable.getIntrinsicHeight()!=attachment.getHeight())){
			photo.setImageDrawable(null);
			photo.setImageDrawable(crossfadeDrawable);
		}
		if(failedOverlay!=null && failedOverlay.getVisibility()!=View.GONE){
			V.setVisibilityAnimated(failedOverlay, View.GONE);
		}
	}

	public void clearImage(){
		crossfadeDrawable.setCrossfadeAlpha(1f);
		crossfadeDrawable.setImageDrawable(null);
		didClear=true;
		if(failedOverlay!=null && failedOverlay.getVisibility()!=View.GONE){
			V.setVisibilityAnimated(failedOverlay, View.GONE);
		}
	}

	public void showFailedOverlay(){
		if(failedOverlay!=null){
			V.setVisibilityAnimated(failedOverlay, View.VISIBLE);
		}
	}

	public boolean isFailedOverlayShown(){
		return failedOverlay!=null && failedOverlay.getVisibility()!=View.GONE;
	}
}
