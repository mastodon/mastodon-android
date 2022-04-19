package org.joinmastodon.android.ui.photoviewer;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Attachment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;

public class PhotoViewer implements ZoomPanView.Listener{
	private static final String TAG="PhotoViewer";

	private Activity activity;
	private List<Attachment> attachments;
	private int currentIndex;
	private WindowManager wm;
	private Listener listener;

	private FrameLayout windowView;
	private ViewPager2 pager;
	private ColorDrawable background=new ColorDrawable(0xff000000);
	private ArrayList<MediaPlayer> players=new ArrayList<>();
	private int screenOnRefCount=0;

	public PhotoViewer(Activity activity, List<Attachment> attachments, int index, Listener listener){
		this.activity=activity;
		this.attachments=attachments.stream().filter(a->a.type==Attachment.Type.IMAGE || a.type==Attachment.Type.GIFV || a.type==Attachment.Type.VIDEO).collect(Collectors.toList());
		currentIndex=index;
		this.listener=listener;

		wm=activity.getWindowManager();

		windowView=new FrameLayout(activity){
			@Override
			public boolean dispatchKeyEvent(KeyEvent event){
				if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){
					if(event.getAction()==KeyEvent.ACTION_DOWN){
						onStartSwipeToDismissTransition(0f);
					}
					return true;
				}
				return false;
			}

			@Override
			public WindowInsets dispatchApplyWindowInsets(WindowInsets insets){
				Log.w(TAG, "dispatchApplyWindowInsets() called with: insets = ["+insets+"]");
				return insets.consumeSystemWindowInsets();
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
		wlp.format=PixelFormat.TRANSLUCENT;
		wlp.setTitle(activity.getString(R.string.media_viewer));
		windowView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		wm.addView(windowView, wlp);

		windowView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				windowView.getViewTreeObserver().removeOnPreDrawListener(this);

				Rect rect=new Rect();
				int[] radius=new int[4];
				if(listener.startPhotoViewTransition(index, rect, radius)){
					RecyclerView rv=(RecyclerView) pager.getChildAt(0);
					BaseHolder holder=(BaseHolder) rv.findViewHolderForAdapterPosition(index);
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
		windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() | (activity.getWindow().getDecorView().getSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)));
		wm.updateViewLayout(windowView, wlp);

		int index=pager.getCurrentItem();
		listener.setPhotoViewVisibility(index, true);
		Rect rect=new Rect();
		int[] radius=new int[4];
		if(listener.startPhotoViewTransition(index, rect, radius)){
			RecyclerView rv=(RecyclerView) pager.getChildAt(0);
			BaseHolder holder=(BaseHolder) rv.findViewHolderForAdapterPosition(index);
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
		for(MediaPlayer player:players)
			player.release();
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

	private void incKeepScreenOn(){
		if(screenOnRefCount==0){
			WindowManager.LayoutParams wlp=(WindowManager.LayoutParams) windowView.getLayoutParams();
			wlp.flags|=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			wm.updateViewLayout(windowView, wlp);
		}
		screenOnRefCount++;
	}

	private void decKeepScreenOn(){
		screenOnRefCount--;
		if(screenOnRefCount<0)
			throw new IllegalStateException();
		if(screenOnRefCount==0){
			WindowManager.LayoutParams wlp=(WindowManager.LayoutParams) windowView.getLayoutParams();
			wlp.flags&=~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			wm.updateViewLayout(windowView, wlp);
		}
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

	private class PhotoViewAdapter extends RecyclerView.Adapter<BaseHolder>{

		@NonNull
		@Override
		public BaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new PhotoViewHolder();
				case 1 -> new GifVViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(@NonNull BaseHolder holder, int position){
			holder.bind(attachments.get(position));
		}

		@Override
		public int getItemCount(){
			return attachments.size();
		}

		@Override
		public int getItemViewType(int position){
			Attachment att=attachments.get(position);
			return switch(att.type){
				case IMAGE -> 0;
				case GIFV, VIDEO -> 1;
				default -> throw new IllegalStateException("Unexpected value: "+att.type);
			};
		}

		@Override
		public void onViewDetachedFromWindow(@NonNull BaseHolder holder){
			super.onViewDetachedFromWindow(holder);
			if(holder instanceof GifVViewHolder gifHolder){
				gifHolder.reset();
			}
		}

		@Override
		public void onViewAttachedToWindow(@NonNull BaseHolder holder){
			super.onViewAttachedToWindow(holder);
			if(holder instanceof GifVViewHolder gifHolder){
				gifHolder.prepareAndStartPlayer();
			}
		}
	}

	private abstract class BaseHolder extends BindableViewHolder<Attachment>{
		public ZoomPanView zoomPanView;
		public BaseHolder(){
			super(new ZoomPanView(activity));
			zoomPanView=(ZoomPanView) itemView;
			zoomPanView.setListener(PhotoViewer.this);
			zoomPanView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}

		@Override
		public void onBind(Attachment item){
			zoomPanView.setScrollDirections(getAbsoluteAdapterPosition()>0, getAbsoluteAdapterPosition()<attachments.size()-1);
		}
	}

	private class PhotoViewHolder extends BaseHolder implements ViewImageLoader.Target{
		public ImageView imageView;

		public PhotoViewHolder(){
			imageView=new ImageView(activity);
			zoomPanView.addView(imageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		}

		@Override
		public void onBind(Attachment item){
			super.onBind(item);
			FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) imageView.getLayoutParams();
			params.width=item.getWidth();
			params.height=item.getHeight();
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

	private class GifVViewHolder extends BaseHolder implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, TextureView.SurfaceTextureListener{
		public TextureView textureView;
		public FrameLayout wrap;
		public MediaPlayer player;
		private Surface surface;
		private boolean playerReady;
		private boolean keepingScreenOn;

		public GifVViewHolder(){
			textureView=new TextureView(activity);
			wrap=new FrameLayout(activity);
			zoomPanView.addView(wrap, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			wrap.addView(textureView);

			textureView.setSurfaceTextureListener(this);
		}

		@Override
		public void onBind(Attachment item){
			super.onBind(item);
			playerReady=false;
			FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) wrap.getLayoutParams();
			params.width=item.getWidth();
			params.height=item.getHeight();
			wrap.setBackground(listener.getPhotoViewCurrentDrawable(getAbsoluteAdapterPosition()));
			if(itemView.isAttachedToWindow()){
				reset();
				prepareAndStartPlayer();
			}
		}

		@Override
		public void onPrepared(MediaPlayer mp){
			Log.d(TAG, "onPrepared() called with: mp = ["+mp+"]");
			playerReady=true;
			if(surface!=null)
				startPlayer();
		}

		@Override
		public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height){
			this.surface=new Surface(surface);
			if(playerReady)
				startPlayer();
		}

		@Override
		public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height){

		}

		@Override
		public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface){
			this.surface=null;
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface){

		}

		private void startPlayer(){
			player.setSurface(surface);
			player.setLooping(true);
			player.start();
			if(item.type==Attachment.Type.VIDEO){
				incKeepScreenOn();
				keepingScreenOn=true;
			}else{
				keepingScreenOn=false;
			}
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra){
			Log.e(TAG, "gif player onError() called with: mp = ["+mp+"], what = ["+what+"], extra = ["+extra+"]");
			return false;
		}

		public void prepareAndStartPlayer(){
			playerReady=false;
			player=new MediaPlayer();
			players.add(player);
			player.setOnPreparedListener(this);
			player.setOnErrorListener(this);
			try{
				player.setDataSource(activity, Uri.parse(item.url));
				player.prepareAsync();
			}catch(IOException x){
				Log.w(TAG, "Error initializing gif player", x);
			}
		}

		public void reset(){
			playerReady=false;
			player.release();
			players.remove(player);
			player=null;
			if(keepingScreenOn){
				decKeepScreenOn();
				keepingScreenOn=false;
			}
		}
	}
}
