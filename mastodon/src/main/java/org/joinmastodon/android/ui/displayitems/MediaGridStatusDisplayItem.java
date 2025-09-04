package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.Translation;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.photoviewer.AltTextSheet;
import org.joinmastodon.android.ui.utils.MediaAttachmentViewController;
import org.joinmastodon.android.ui.views.FrameLayoutThatOnlyMeasuresFirstChild;
import org.joinmastodon.android.ui.views.MaxWidthFrameLayout;
import org.joinmastodon.android.ui.views.MediaGridLayout;
import org.joinmastodon.android.utils.TypedObjectPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class MediaGridStatusDisplayItem extends StatusDisplayItem{
	private static final String TAG="MediaGridDisplayItem";

	private final PhotoLayoutHelper.TiledLayoutResult tiledLayout;
	private final TypedObjectPool<GridItemType, MediaAttachmentViewController> viewPool;
	private final List<Attachment> attachments;
	private final Map<String, Pair<String, String>> translatedAttachments = new HashMap<>();
	private final ArrayList<ImageLoaderRequest> requests=new ArrayList<>();
	public final Status status;
	public boolean sensitiveRevealed;
	public String sensitiveTitle;

	public MediaGridStatusDisplayItem(String parentID, Callbacks callbacks, Context context, PhotoLayoutHelper.TiledLayoutResult tiledLayout, List<Attachment> attachments, Status status){
		super(parentID, callbacks, context);
		this.tiledLayout=tiledLayout;
		this.viewPool=callbacks.getAttachmentViewsPool();
		this.attachments=attachments;
		this.status=status;
		sensitiveRevealed=!status.sensitive;
		for(Attachment att:attachments){
			requests.add(new UrlImageLoaderRequest(switch(att.type){
				case IMAGE -> att.url;
				case VIDEO, GIFV -> att.previewUrl;
				default -> throw new IllegalStateException("Unexpected value: "+att.type);
			}, 1000, 1000));
		}
	}

	@Override
	public Type getType(){
		return Type.MEDIA_GRID;
	}

	@Override
	public int getImageCount(){
		return requests.size();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return requests.get(index);
	}

	public enum GridItemType{
		PHOTO,
		VIDEO,
		GIFV
	}

	public static class Holder extends StatusDisplayItem.Holder<MediaGridStatusDisplayItem> implements ImageLoaderViewHolder{
		private final FrameLayout wrapper;
		private final MediaGridLayout layout;
		private final View.OnClickListener clickListener=this::onViewClick, altTextClickListener=this::onAltTextClick;
		private final ArrayList<MediaAttachmentViewController> controllers=new ArrayList<>();

		private final MaxWidthFrameLayout overlays;

		private final View sensitiveOverlay;
		private final LayerDrawable sensitiveOverlayBG;
		private static final ColorDrawable drawableForWhenThereIsNoBlurhash=new ColorDrawable(0xffffffff);
		private final TextView hideSensitiveButton;
		private final TextView sensitiveText;
		private boolean thereAreFailedImages;

		public Holder(Activity activity, ViewGroup parent){
			super(new FrameLayoutThatOnlyMeasuresFirstChild(activity));
			wrapper=(FrameLayout)itemView;
			layout=new MediaGridLayout(activity);
			wrapper.addView(layout);
			wrapper.setClipToPadding(false);

			overlays=new MaxWidthFrameLayout(activity);
			overlays.setMaxWidth(V.dp(MediaGridLayout.MAX_WIDTH));
			wrapper.addView(overlays, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));

			hideSensitiveButton=(TextView) activity.getLayoutInflater().inflate(R.layout.alt_text_badge, overlays, false);
			hideSensitiveButton.setText(R.string.hide);
			FrameLayout.LayoutParams lp;
			overlays.addView(hideSensitiveButton, lp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(24), Gravity.END | Gravity.BOTTOM));
			int margin=V.dp(8);
			lp.setMargins(margin, margin, margin, margin);

			activity.getLayoutInflater().inflate(R.layout.overlay_image_sensitive, overlays);
			sensitiveOverlay=findViewById(R.id.sensitive_overlay);
			sensitiveOverlayBG=(LayerDrawable) sensitiveOverlay.getBackground().mutate();
			sensitiveOverlayBG.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable(false));
			sensitiveOverlayBG.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable(true));
			sensitiveOverlay.setBackground(sensitiveOverlayBG);
			sensitiveOverlay.setOnClickListener(v->revealSensitive());
			sensitiveOverlay.setOutlineProvider(OutlineProviders.roundedRect(8));
			sensitiveOverlay.setClipToOutline(true);
			hideSensitiveButton.setOnClickListener(v->hideSensitive());

			sensitiveText=findViewById(R.id.sensitive_text);
		}

		@Override
		public void onBind(MediaGridStatusDisplayItem item){
			thereAreFailedImages=false;
			wrapper.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, V.dp(16), V.dp(8));

			layout.setTiledLayout(item.tiledLayout);
			for(MediaAttachmentViewController c:controllers){
				item.viewPool.reuse(c.type, c);
			}
			layout.removeAllViews();
			controllers.clear();
			int i=0;
			for(Attachment att:item.attachments){
				MediaAttachmentViewController c=item.viewPool.obtain(switch(att.type){
					case IMAGE -> GridItemType.PHOTO;
					case VIDEO -> GridItemType.VIDEO;
					case GIFV -> GridItemType.GIFV;
					default -> throw new IllegalStateException("Unexpected value: "+att.type);
				});
				if(c.view.getLayoutParams()==null)
					c.view.setLayoutParams(new MediaGridLayout.LayoutParams(item.tiledLayout.tiles[i]));
				else
					((MediaGridLayout.LayoutParams) c.view.getLayoutParams()).tile=item.tiledLayout.tiles[i];
				layout.addView(c.view);
				c.view.setOnClickListener(clickListener);
				c.view.setTag(i);
				if(c.altButton!=null){
					c.altButton.setOnClickListener(altTextClickListener);
					c.altButton.setTag(i);
					c.altButton.setAlpha(1f);
				}
				controllers.add(c);

				if (item.status.translation!=null && item.status.translation.mediaAttachments!=null){
					if(item.status.translationState==Status.TranslationState.SHOWN){
						if(!item.translatedAttachments.containsKey(att.id)){
							Optional<Translation.MediaAttachment> translatedAttachment=Arrays.stream(item.status.translation.mediaAttachments).filter(mediaAttachment->mediaAttachment.id.equals(att.id)).findFirst();
							translatedAttachment.ifPresent(mediaAttachment->{
								item.translatedAttachments.put(mediaAttachment.id, new Pair<>(att.description, mediaAttachment.description));
								att.description=mediaAttachment.description;
							});
						}else{
							//SAFETY: must be non-null, as we check if the map contains the attachment before
							att.description=Objects.requireNonNull(item.translatedAttachments.get(att.id)).second;
						}
					}else{
						if (item.translatedAttachments.containsKey(att.id)) {
							att.description=Objects.requireNonNull(item.translatedAttachments.get(att.id)).first;
						}
					}
				}
				c.bind(att, item.status);
				i++;
			}

			if(!item.sensitiveRevealed){
				sensitiveOverlay.setVisibility(View.VISIBLE);
				layout.setVisibility(View.INVISIBLE);
				updateBlurhashInSensitiveOverlay();
			}else{
				sensitiveOverlay.setVisibility(View.GONE);
				layout.setVisibility(View.VISIBLE);
			}
			hideSensitiveButton.setVisibility(item.status.sensitive ? View.VISIBLE : View.GONE);
			if(!TextUtils.isEmpty(item.sensitiveTitle))
				sensitiveText.setText(item.sensitiveTitle);
			else
				sensitiveText.setText(R.string.sensitive_content_explain);
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index<controllers.size())
				controllers.get(index).setImage(drawable);
		}

		@Override
		public void clearImage(int index){
			if(index<controllers.size())
				controllers.get(index).clearImage();
		}

		@Override
		public void onImageLoadingFailed(int index, Throwable error){
			if(index<controllers.size()){
				controllers.get(index).showFailedOverlay();
				thereAreFailedImages=true;
			}
		}

		private void onViewClick(View v){
			int index=(Integer)v.getTag();
			item.callbacks.openPhotoViewer(item.parentID, item.status, index, this);
			if(thereAreFailedImages){
				for(MediaAttachmentViewController controller:controllers){
					if(controller.isFailedOverlayShown()){
						controller.clearImage();
					}
				}
				item.callbacks.retryFailedImages();
			}
		}

		private void onAltTextClick(View v){
			int index=(Integer)v.getTag();
			Attachment att=item.attachments.get(index);
			new AltTextSheet(v.getContext(), att).show();
		}

		public MediaAttachmentViewController getViewController(int index){
			return controllers.get(index);
		}

		public void setClipChildren(boolean clip){
			layout.setClipChildren(clip);
			wrapper.setClipChildren(clip);
		}

		private void updateBlurhashInSensitiveOverlay(){
			Drawable d=item.attachments.get(0).blurhashPlaceholder;
			sensitiveOverlayBG.setDrawableByLayerId(R.id.blurhash, d==null ? drawableForWhenThereIsNoBlurhash : d.mutate());
		}

		private void revealSensitive(){
			if(item.sensitiveRevealed)
				return;
			item.sensitiveRevealed=true;
			V.setVisibilityAnimated(sensitiveOverlay, View.GONE);
			layout.setVisibility(View.VISIBLE);
		}

		private void hideSensitive(){
			if(!item.sensitiveRevealed)
				return;
			updateBlurhashInSensitiveOverlay();
			item.sensitiveRevealed=false;
			V.setVisibilityAnimated(sensitiveOverlay, View.VISIBLE, ()->layout.setVisibility(View.INVISIBLE));
		}

		public MediaGridLayout getLayout(){
			return layout;
		}

		public View getSensitiveOverlay(){
			return sensitiveOverlay;
		}
	}
}
