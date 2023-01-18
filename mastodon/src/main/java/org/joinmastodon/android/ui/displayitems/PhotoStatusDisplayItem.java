package org.joinmastodon.android.ui.displayitems;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;

import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class PhotoStatusDisplayItem extends ImageStatusDisplayItem{
	public PhotoStatusDisplayItem(String parentID, Status status, Attachment photo, BaseStatusListFragment parentFragment, int index, int totalPhotos, PhotoLayoutHelper.TiledLayoutResult tiledLayout, PhotoLayoutHelper.TiledLayoutResult.Tile thisTile){
		super(parentID, parentFragment, photo, status, index, totalPhotos, tiledLayout, thisTile);
		request=new UrlImageLoaderRequest(photo.url, 1000, 1000);
	}

	@Override
	public Type getType(){
		return Type.PHOTO;
	}

	public static class Holder extends ImageStatusDisplayItem.Holder<PhotoStatusDisplayItem>{
		private final FrameLayout altTextWrapper;
		private final TextView altTextButton;
		private final View altTextScroller;
		private final ImageButton altTextClose;
		private final TextView altText;

		private boolean altTextShown;
		private AnimatorSet currentAnim;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_photo, parent);
			altTextWrapper=findViewById(R.id.alt_text_wrapper);
			altTextButton=findViewById(R.id.alt_button);
			altTextScroller=findViewById(R.id.alt_text_scroller);
			altTextClose=findViewById(R.id.alt_text_close);
			altText=findViewById(R.id.alt_text);

			altTextButton.setOnClickListener(this::onShowHideClick);
			altTextClose.setOnClickListener(this::onShowHideClick);
//			altTextScroller.setNestedScrollingEnabled(true);
		}

		@Override
		public void onBind(ImageStatusDisplayItem item){
			super.onBind(item);
			altTextShown=false;
			if(currentAnim!=null)
				currentAnim.cancel();
			altTextScroller.setVisibility(View.GONE);
			altTextClose.setVisibility(View.GONE);
			altTextButton.setVisibility(View.VISIBLE);
			if(TextUtils.isEmpty(item.attachment.description)){
				altTextWrapper.setVisibility(View.GONE);
			}else{
				altTextWrapper.setVisibility(View.VISIBLE);
				altText.setText(item.attachment.description);
			}
		}

		private void onShowHideClick(View v){
			boolean show=v.getId()==R.id.alt_button;

			if(altTextShown==show)
				return;
			if(currentAnim!=null)
				currentAnim.cancel();

			altTextShown=show;
			if(show){
				altTextScroller.setVisibility(View.VISIBLE);
				altTextClose.setVisibility(View.VISIBLE);
			}else{
				altTextButton.setVisibility(View.VISIBLE);
				// Hide these views temporarily so FrameLayout measures correctly
				altTextScroller.setVisibility(View.GONE);
				altTextClose.setVisibility(View.GONE);
			}

			// This is the current size...
			int prevLeft=altTextWrapper.getLeft();
			int prevRight=altTextWrapper.getRight();
			int prevTop=altTextWrapper.getTop();
			altTextWrapper.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
				@Override
				public boolean onPreDraw(){
					altTextWrapper.getViewTreeObserver().removeOnPreDrawListener(this);

					// ...and this is after the layout pass, right now the FrameLayout has its final size, but we animate that change
					if(!show){
						// Show these views again so they're visible for the duration of the animation.
						// No one would notice they were missing during measure/layout.
						altTextScroller.setVisibility(View.VISIBLE);
						altTextClose.setVisibility(View.VISIBLE);
					}
					AnimatorSet set=new AnimatorSet();
					set.playTogether(
							ObjectAnimator.ofInt(altTextWrapper, "left", prevLeft, altTextWrapper.getLeft()),
							ObjectAnimator.ofInt(altTextWrapper, "right", prevRight, altTextWrapper.getRight()),
							ObjectAnimator.ofInt(altTextWrapper, "top", prevTop, altTextWrapper.getTop()),
							ObjectAnimator.ofFloat(altTextButton, View.ALPHA, show ? 1f : 0f, show ? 0f : 1f),
							ObjectAnimator.ofFloat(altTextScroller, View.ALPHA, show ? 0f : 1f, show ? 1f : 0f),
							ObjectAnimator.ofFloat(altTextClose, View.ALPHA, show ? 0f : 1f, show ? 1f : 0f)
					);
					set.setDuration(300);
					set.setInterpolator(CubicBezierInterpolator.DEFAULT);
					set.addListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation){
							if(show){
								altTextButton.setVisibility(View.GONE);
							}else{
								altTextScroller.setVisibility(View.GONE);
								altTextClose.setVisibility(View.GONE);
							}
							currentAnim=null;
						}
					});
					set.start();
					currentAnim=set;

					return true;
				}
			});
		}
	}
}
