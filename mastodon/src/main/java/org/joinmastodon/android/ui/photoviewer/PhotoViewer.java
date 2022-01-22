package org.joinmastodon.android.ui.photoviewer;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.joinmastodon.android.model.Attachment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class PhotoViewer implements ZoomPanView.Listener{
	private Activity activity;
	private List<Attachment> attachments;
	private int currentIndex;
	private WindowManager wm;
	private Listener listener;

	private FrameLayout windowView;
	private ViewPager2 pager;
	private ColorDrawable background=new ColorDrawable(0xff000000);

	public PhotoViewer(Activity activity, List<Attachment> attachments, int index, Listener listener){
		this.activity=activity;
		this.attachments=attachments;
		currentIndex=index;
		this.listener=listener;

		wm=activity.getWindowManager();

		windowView=new FrameLayout(activity){
			@Override
			public boolean dispatchKeyEvent(KeyEvent event){
				if(event.getAction()==KeyEvent.ACTION_DOWN && event.getKeyCode()==KeyEvent.KEYCODE_BACK){
					onStartSwipeToDismissTransition(0f);
				}
				return true;
			}
		};
		windowView.setBackground(background);
		background.setAlpha(0);
		pager=new ViewPager2(activity);
		pager.setAdapter(new PhotoViewAdapter());
		pager.setCurrentItem(index, false);
		windowView.addView(pager);
		pager.setMotionEventSplittingEnabled(false);

		WindowManager.LayoutParams wlp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
		wlp.type=WindowManager.LayoutParams.TYPE_APPLICATION;
		wlp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
				| WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
		wlp.format=PixelFormat.RGBA_8888;
		wm.addView(windowView, wlp);

		windowView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				windowView.getViewTreeObserver().removeOnPreDrawListener(this);

				Rect rect=new Rect();
				int[] radius=new int[4];
				if(listener.startPhotoViewTransition(index, rect, radius)){
					RecyclerView rv=(RecyclerView) pager.getChildAt(0);
					PhotoViewHolder holder=(PhotoViewHolder) rv.findViewHolderForAdapterPosition(index);
					holder.zoomPanView.animateIn(rect, radius);
				}

				return true;
			}
		});
	}

	@Override
	public void onTransitionAnimationUpdate(float translateX, float translateY, float scale){
		listener.setTransitioningViewTransform(translateX, translateY, scale);
	}

	@Override
	public void onTransitionAnimationFinished(){
		listener.endPhotoViewTransition();
	}

	@Override
	public void onSetBackgroundAlpha(float alpha){
		background.setAlpha(Math.round(alpha*255f));
	}

	@Override
	public void onStartSwipeToDismiss(){
		listener.setPhotoViewVisibility(pager.getCurrentItem(), false);
	}

	@Override
	public void onStartSwipeToDismissTransition(float velocityY){
		// stop receiving input events to allow the user to interact with the underlying UI while the animation is still running
		WindowManager.LayoutParams wlp=(WindowManager.LayoutParams) windowView.getLayoutParams();
		wlp.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		wm.updateViewLayout(windowView, wlp);

		int index=pager.getCurrentItem();
		listener.setPhotoViewVisibility(index, true);
		Rect rect=new Rect();
		int[] radius=new int[4];
		if(listener.startPhotoViewTransition(index, rect, radius)){
			RecyclerView rv=(RecyclerView) pager.getChildAt(0);
			PhotoViewHolder holder=(PhotoViewHolder) rv.findViewHolderForAdapterPosition(index);
			holder.zoomPanView.animateOut(rect, radius, velocityY);
		}else{
			windowView.animate()
					.alpha(0)
					.setDuration(300)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
					.withEndAction(()->wm.removeView(windowView))
					.start();
		}
	}

	@Override
	public void onSwipeToDismissCanceled(){
		listener.setPhotoViewVisibility(pager.getCurrentItem(), true);
	}

	@Override
	public void onDismissed(){
		listener.setPhotoViewVisibility(pager.getCurrentItem(), true);
		wm.removeView(windowView);
		listener.photoViewerDismissed();
	}

	/**
	 * To be called when the list containing photo views is scrolled
	 * @param x
	 * @param y
	 */
	public void offsetView(float x, float y){
		pager.setTranslationX(pager.getTranslationX()+x);
		pager.setTranslationY(pager.getTranslationY()+y);
	}

	public interface Listener{
		void setPhotoViewVisibility(int index, boolean visible);

		/**
		 * Find a view for transition, save a reference to it until <code>{@link #endPhotoViewTransition()}</code> is called,
		 * and set up the view hierarchy for transition (the photo view may need to be drawn outside of the bounds of its parent).
		 * @param index the index of the photo/page
		 * @param outRect output: the rect of the photo view <b>in screen coordinates</b>
		 * @param outCornerRadius output: corner radiuses of the view [top-left, top-right, bottom-right, bottom-left]
		 * @return true if the view was found and outRect and outCornerRadius are valid
		 */
		boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius);

		/**
		 * Update the transformation parameters of the transitioning photo view.
		 * Only called if a previous call to {@link #startPhotoViewTransition(int, Rect, int[])} returned true.
		 * @param translateX X translation
		 * @param translateY Y translation
		 * @param scale X and Y scale
		 */
		void setTransitioningViewTransform(float translateX, float translateY, float scale);

		/**
		 * End the transition, returning all transformations to their initial state.
		 */
		void endPhotoViewTransition();

		/**
		 * Get the current drawable that a photo view displays.
		 * @param index the index of the photo
		 * @return the drawable, or null if the view doesn't exist
		 */
		@Nullable
		Drawable getPhotoViewCurrentDrawable(int index);

		void photoViewerDismissed();
	}

	private class PhotoViewAdapter extends RecyclerView.Adapter<PhotoViewHolder>{

		@NonNull
		@Override
		public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new PhotoViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position){
			holder.bind(attachments.get(position));
		}

		@Override
		public int getItemCount(){
			return attachments.size();
		}
	}

	private class PhotoViewHolder extends BindableViewHolder<Attachment> implements ViewImageLoader.Target{
		public ImageView imageView;
		public ZoomPanView zoomPanView;

		public PhotoViewHolder(){
			super(new ZoomPanView(activity));
			zoomPanView=(ZoomPanView) itemView;
			zoomPanView.setListener(PhotoViewer.this);
			itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			imageView=new ImageView(activity);
			((FrameLayout)itemView).addView(imageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		}

		@Override
		public void onBind(Attachment item){
			FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) imageView.getLayoutParams();
			params.width=item.getWidth();
			params.height=item.getHeight();
			zoomPanView.setScrollDirections(getAbsoluteAdapterPosition()>0, getAbsoluteAdapterPosition()<attachments.size()-1);
			ViewImageLoader.load(this, listener.getPhotoViewCurrentDrawable(getAbsoluteAdapterPosition()), new UrlImageLoaderRequest(item.url), false);
		}

		@Override
		public void setImageDrawable(Drawable d){
			imageView.setImageDrawable(d);
		}

		@Override
		public View getView(){
			return imageView;
		}
	}
}
