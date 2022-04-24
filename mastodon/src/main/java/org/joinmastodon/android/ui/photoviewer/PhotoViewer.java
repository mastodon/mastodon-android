package org.joinmastodon.android.ui.photoviewer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Source;

public class PhotoViewer implements ZoomPanView.Listener{
	private static final String TAG="PhotoViewer";
	public static final int PERMISSION_REQUEST=926;

	private Activity activity;
	private List<Attachment> attachments;
	private int currentIndex;
	private WindowManager wm;
	private Listener listener;

	private FrameLayout windowView;
	private FragmentRootLinearLayout uiOverlay;
	private ViewPager2 pager;
	private ColorDrawable background=new ColorDrawable(0xff000000);
	private ArrayList<MediaPlayer> players=new ArrayList<>();
	private int screenOnRefCount=0;
	private Toolbar toolbar;
	private View toolbarWrap;
	private SeekBar videoSeekBar;
	private TextView videoTimeView;
	private ImageButton videoPlayPauseButton;
	private View videoControls;
	private boolean uiVisible=true;
	private AudioManager.OnAudioFocusChangeListener audioFocusListener=this::onAudioFocusChanged;
	private Runnable uiAutoHider=()->{
		if(uiVisible)
			toggleUI();
	};

	private boolean videoPositionNeedsUpdating;
	private Runnable videoPositionUpdater=this::updateVideoPosition;
	private int videoDuration, videoInitialPosition, videoLastTimeUpdatePosition;
	private long videoInitialPositionTime;

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
				if(Build.VERSION.SDK_INT>=29){
					DisplayCutout cutout=insets.getDisplayCutout();
					if(cutout!=null){
						// Make controls extend beneath the cutout, and replace insets to avoid cutout insets being filled with "navigation bar color"
						Insets tappable=insets.getTappableElementInsets();
						int leftInset=Math.max(0, cutout.getSafeInsetLeft()-tappable.left);
						int rightInset=Math.max(0, cutout.getSafeInsetRight()-tappable.right);
						insets=insets.replaceSystemWindowInsets(tappable.left, tappable.top, tappable.right, tappable.bottom);
						toolbarWrap.setPadding(leftInset, 0, rightInset, 0);
						videoControls.setPadding(leftInset, 0, rightInset, 0);
					}else{
						toolbarWrap.setPadding(0, 0, 0, 0);
						videoControls.setPadding(0, 0, 0, 0);
					}
				}
				uiOverlay.dispatchApplyWindowInsets(insets);
				int bottomInset=insets.getSystemWindowInsetBottom();
				if(bottomInset>0 && bottomInset<V.dp(36)){
					uiOverlay.setPadding(uiOverlay.getPaddingLeft(), uiOverlay.getPaddingTop(), uiOverlay.getPaddingRight(), V.dp(36));
				}
				return insets.consumeSystemWindowInsets();
			}
		};
		windowView.setBackground(background);
		background.setAlpha(0);
		pager=new ViewPager2(activity);
		pager.setAdapter(new PhotoViewAdapter());
		pager.setCurrentItem(index, false);
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageSelected(int position){
				onPageChanged(position);
			}
		});
		windowView.addView(pager);
		pager.setMotionEventSplittingEnabled(false);

		uiOverlay=activity.getLayoutInflater().inflate(R.layout.photo_viewer_ui, windowView).findViewById(R.id.photo_viewer_overlay);
		uiOverlay.setStatusBarColor(0x80000000);
		uiOverlay.setNavigationBarColor(0x80000000);
		toolbarWrap=uiOverlay.findViewById(R.id.toolbar_wrap);
		toolbar=uiOverlay.findViewById(R.id.toolbar);
		toolbar.setNavigationOnClickListener(v->onStartSwipeToDismissTransition(0));
		toolbar.getMenu().add(R.string.download).setIcon(R.drawable.ic_fluent_arrow_download_24_regular).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		toolbar.setOnMenuItemClickListener(item->{
			saveCurrentFile();
			return true;
		});
		uiOverlay.setAlpha(0f);
		videoControls=uiOverlay.findViewById(R.id.video_player_controls);
		videoSeekBar=uiOverlay.findViewById(R.id.seekbar);
		videoTimeView=uiOverlay.findViewById(R.id.time);
		videoPlayPauseButton=uiOverlay.findViewById(R.id.play_pause_btn);
		if(attachments.get(index).type!=Attachment.Type.VIDEO){
			videoControls.setVisibility(View.GONE);
		}else{
			videoDuration=(int)Math.round(attachments.get(index).getDuration()*1000);
			videoLastTimeUpdatePosition=-1;
			updateVideoTimeText(0);
		}

		WindowManager.LayoutParams wlp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
		wlp.type=WindowManager.LayoutParams.TYPE_APPLICATION;
		wlp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
				| WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
		wlp.format=PixelFormat.TRANSLUCENT;
		wlp.setTitle(activity.getString(R.string.media_viewer));
		if(Build.VERSION.SDK_INT>=28)
			wlp.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30 ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
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

		videoPlayPauseButton.setOnClickListener(v->{
			MediaPlayer player=findCurrentVideoPlayer();
			if(player!=null){
				if(player.isPlaying())
					pauseVideo();
				else
					resumeVideo();
				hideUiDelayed();
			}
		});
		videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
				if(fromUser){
					float p=progress/10000f;
					updateVideoTimeText(Math.round(p*videoDuration));
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar){
				stopUpdatingVideoPosition();
				if(!uiVisible) // If dragging started during hide animation
					toggleUI();
				windowView.removeCallbacks(uiAutoHider);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar){
				MediaPlayer player=findCurrentVideoPlayer();
				if(player!=null){
					float progress=seekBar.getProgress()/10000f;
					player.seekTo(Math.round(progress*player.getDuration()));
				}
				hideUiDelayed();
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
		uiOverlay.setAlpha(Math.max(0f, alpha*2f-1f));
	}

	@Override
	public void onStartSwipeToDismiss(){
		listener.setPhotoViewVisibility(pager.getCurrentItem(), false);
		if(!uiVisible){
			windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() & ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN));
		}else{
			windowView.removeCallbacks(uiAutoHider);
		}
	}

	@Override
	public void onStartSwipeToDismissTransition(float velocityY){
		pauseVideo();
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
		if(!uiVisible){
			windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
		}else if(attachments.get(currentIndex).type==Attachment.Type.VIDEO){
			hideUiDelayed();
		}
	}

	@Override
	public void onDismissed(){
		for(MediaPlayer player:players)
			player.release();
		if(!players.isEmpty()){
			activity.getSystemService(AudioManager.class).abandonAudioFocus(audioFocusListener);
		}
		listener.setPhotoViewVisibility(pager.getCurrentItem(), true);
		wm.removeView(windowView);
		listener.photoViewerDismissed();
	}

	@Override
	public void onSingleTap(){
		toggleUI();
	}

	private void toggleUI(){
		if(uiVisible){
			uiOverlay.animate()
					.alpha(0f)
					.setDuration(250)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
					.withEndAction(()->uiOverlay.setVisibility(View.GONE))
					.start();
			windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
		}else{
			uiOverlay.setVisibility(View.VISIBLE);
			uiOverlay.animate()
					.alpha(1f)
					.setDuration(300)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
					.start();
			windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() & ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN));
			if(attachments.get(currentIndex).type==Attachment.Type.VIDEO)
				hideUiDelayed(5000);
		}
		uiVisible=!uiVisible;
	}

	private void hideUiDelayed(){
		hideUiDelayed(2000);
	}

	private void hideUiDelayed(long delay){
		windowView.removeCallbacks(uiAutoHider);
		windowView.postDelayed(uiAutoHider, delay);
	}

	private void onPageChanged(int index){
		currentIndex=index;
		Attachment att=attachments.get(index);
		V.setVisibilityAnimated(videoControls, att.type==Attachment.Type.VIDEO ? View.VISIBLE : View.GONE);
		if(att.type==Attachment.Type.VIDEO){
			videoSeekBar.setSecondaryProgress(0);
			videoDuration=(int)Math.round(att.getDuration()*1000);
			videoLastTimeUpdatePosition=-1;
			updateVideoTimeText(0);
		}
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
			activity.getSystemService(AudioManager.class).requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
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
			activity.getSystemService(AudioManager.class).abandonAudioFocus(audioFocusListener);
		}
	}

	public void onPause(){
		pauseVideo();
	}

	private void saveCurrentFile(){
		if(Build.VERSION.SDK_INT>=29){
			doSaveCurrentFile();
		}else{
			if(activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
				listener.onRequestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
			}else{
				doSaveCurrentFile();
			}
		}
	}

	public void onRequestPermissionsResult(String[] permissions, int[] results){
		if(results[0]==PackageManager.PERMISSION_GRANTED){
			doSaveCurrentFile();
		}else if(!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
			new M3AlertDialogBuilder(activity)
					.setTitle(R.string.permission_required)
					.setMessage(R.string.storage_permission_to_download)
					.setPositiveButton(R.string.open_settings, (dialog, which)->activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.getPackageName(), null))))
					.setNegativeButton(R.string.cancel, null)
					.show();
		}
	}

	private String mimeTypeForFileName(String fileName){
		int extOffset=fileName.lastIndexOf('.');
		if(extOffset>0){
			return switch(fileName.substring(extOffset+1).toLowerCase()){
				case "jpg", "jpeg" -> "image/jpeg";
				case "png" -> "image/png";
				case "gif" -> "image/gif";
				case "webp" -> "image/webp";
				case "mp4" -> "video/mp4";
				case "webm" -> "video/webm";
				default -> null;
			};
		}
		return null;
	}

	private OutputStream destinationStreamForFile(Attachment att) throws IOException{
		String fileName=Uri.parse(att.url).getLastPathSegment();
		if(Build.VERSION.SDK_INT>=29){
			ContentValues values=new ContentValues();
//			values.put(MediaStore.Downloads.DOWNLOAD_URI, att.url);
			values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
			values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
			String mime=mimeTypeForFileName(fileName);
			if(mime!=null)
				values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
			ContentResolver cr=activity.getContentResolver();
			Uri itemUri=cr.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
			return cr.openOutputStream(itemUri);
		}else{
			return new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName));
		}
	}

	private void doSaveCurrentFile(){
		Attachment att=attachments.get(pager.getCurrentItem());
		if(att.type==Attachment.Type.IMAGE){
			UrlImageLoaderRequest req=new UrlImageLoaderRequest(att.url);
			try{
				File file=ImageCache.getInstance(activity).getFile(req);
				if(file==null){
					saveViaDownloadManager(att);
					return;
				}
				MastodonAPIController.runInBackground(()->{
					try(Source src=Okio.source(file); Sink sink=Okio.sink(destinationStreamForFile(att))){
						BufferedSink buf=Okio.buffer(sink);
						buf.writeAll(src);
						buf.flush();
						activity.runOnUiThread(()->Toast.makeText(activity, R.string.file_saved, Toast.LENGTH_SHORT).show());
						if(Build.VERSION.SDK_INT<29){
							String fileName=Uri.parse(att.url).getLastPathSegment();
							File dstFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
							MediaScannerConnection.scanFile(activity, new String[]{dstFile.getAbsolutePath()}, new String[]{mimeTypeForFileName(fileName)}, null);
						}
					}catch(IOException x){
						Log.w(TAG, "doSaveCurrentFile: ", x);
						activity.runOnUiThread(()->Toast.makeText(activity, R.string.error_saving_file, Toast.LENGTH_SHORT).show());
					}
				});
			}catch(IOException x){
				Log.w(TAG, "doSaveCurrentFile: ", x);
				Toast.makeText(activity, R.string.error_saving_file, Toast.LENGTH_SHORT).show();
			}
		}else{
			saveViaDownloadManager(att);
		}
	}

	private void saveViaDownloadManager(Attachment att){
		Uri uri=Uri.parse(att.url);
		DownloadManager.Request req=new DownloadManager.Request(uri);
		req.allowScanningByMediaScanner();
		req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());
		activity.getSystemService(DownloadManager.class).enqueue(req);
		Toast.makeText(activity, R.string.downloading, Toast.LENGTH_SHORT).show();
	}

	private void onAudioFocusChanged(int change){
		if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
			pauseVideo();
		}
	}

	private MediaPlayer findCurrentVideoPlayer(){
		RecyclerView rv=(RecyclerView) pager.getChildAt(0);
		if(rv.findViewHolderForAdapterPosition(pager.getCurrentItem()) instanceof GifVViewHolder vvh && vvh.playerReady){
			return vvh.player;
		}
		return null;
	}

	private void pauseVideo(){
		MediaPlayer player=findCurrentVideoPlayer();
		if(player==null || !player.isPlaying())
			return;
		player.pause();
		videoPlayPauseButton.setImageResource(R.drawable.ic_play_24);
		videoPlayPauseButton.setContentDescription(activity.getString(R.string.play));
		stopUpdatingVideoPosition();
		windowView.removeCallbacks(uiAutoHider);
	}

	private void resumeVideo(){
		MediaPlayer player=findCurrentVideoPlayer();
		if(player==null || player.isPlaying())
			return;
		player.start();
		videoPlayPauseButton.setImageResource(R.drawable.ic_pause_24);
		videoPlayPauseButton.setContentDescription(activity.getString(R.string.pause));
		startUpdatingVideoPosition(player);
	}

	private void startUpdatingVideoPosition(MediaPlayer player){
		videoInitialPosition=player.getCurrentPosition();
		videoInitialPositionTime=SystemClock.uptimeMillis();
		videoDuration=player.getDuration();
		videoPositionNeedsUpdating=true;
		windowView.postOnAnimation(videoPositionUpdater);
	}

	private void stopUpdatingVideoPosition(){
		videoPositionNeedsUpdating=false;
		windowView.removeCallbacks(videoPositionUpdater);
	}

	private String formatTime(int timeSec, boolean includeHours){
		if(includeHours)
			return String.format(Locale.getDefault(), "%d:%02d:%02d", timeSec/3600, timeSec%3600/60, timeSec%60);
		else
			return String.format(Locale.getDefault(), "%d:%02d", timeSec/60, timeSec%60);
	}

	private void updateVideoPosition(){
		if(videoPositionNeedsUpdating){
			int currentPosition=videoInitialPosition+(int)(SystemClock.uptimeMillis()-videoInitialPositionTime);
			videoSeekBar.setProgress(Math.round((float)currentPosition/videoDuration*10000f));
			updateVideoTimeText(currentPosition);
			windowView.postOnAnimation(videoPositionUpdater);
		}
	}

	@SuppressLint("SetTextI18n")
	private void updateVideoTimeText(int currentPosition){
		int currentPositionSec=currentPosition/1000;
		if(currentPositionSec!=videoLastTimeUpdatePosition){
			videoLastTimeUpdatePosition=currentPositionSec;
			boolean includeHours=videoDuration>=3600_000;
			videoTimeView.setText(formatTime(currentPositionSec, includeHours)+" / "+formatTime(videoDuration/1000, includeHours));
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
		void onRequestPermissions(String[] permissions);
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

	private class GifVViewHolder extends BaseHolder implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
			MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, TextureView.SurfaceTextureListener{
		public TextureView textureView;
		public FrameLayout wrap;
		public MediaPlayer player;
		private Surface surface;
		private boolean playerReady;
		private boolean keepingScreenOn;
		private ProgressBar progressBar;

		public GifVViewHolder(){
			textureView=new TextureView(activity);
			wrap=new FrameLayout(activity);
			zoomPanView.addView(wrap, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
			wrap.addView(textureView);

			progressBar=new ProgressBar(activity);
			progressBar.setIndeterminateTintList(ColorStateList.valueOf(0xffffffff));
			zoomPanView.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

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
			progressBar.setVisibility(item.type==Attachment.Type.VIDEO ? View.VISIBLE : View.GONE);
			if(itemView.isAttachedToWindow()){
				reset();
				prepareAndStartPlayer();
			}
		}

		@Override
		public void onPrepared(MediaPlayer mp){
			Log.d(TAG, "onPrepared() called with: mp = ["+mp+"]");
			playerReady=true;
			progressBar.setVisibility(View.GONE);
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
			if(item.type==Attachment.Type.VIDEO){
				incKeepScreenOn();
				keepingScreenOn=true;
				if(getAbsoluteAdapterPosition()==currentIndex){
					player.start();
					startUpdatingVideoPosition(player);
					hideUiDelayed();
				}
			}else{
				keepingScreenOn=false;
				player.setLooping(true);
				player.start();
			}
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra){
			Log.e(TAG, "video player onError() called with: mp = ["+mp+"], what = ["+what+"], extra = ["+extra+"]");
			return false;
		}

		public void prepareAndStartPlayer(){
			playerReady=false;
			player=new MediaPlayer();
			players.add(player);
			player.setOnPreparedListener(this);
			player.setOnErrorListener(this);
			player.setOnVideoSizeChangedListener(this);
			if(item.type==Attachment.Type.VIDEO){
				player.setOnBufferingUpdateListener(this);
				player.setOnInfoListener(this);
				player.setOnSeekCompleteListener(this);
				player.setOnCompletionListener(this);
			}
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

		@Override
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height){
			FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) wrap.getLayoutParams();
			params.width=width;
			params.height=height;
			zoomPanView.updateLayout();
		}

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent){
			if(getAbsoluteAdapterPosition()==currentIndex){
				videoSeekBar.setSecondaryProgress(percent*100);
			}
		}

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra){
			return switch(what){
				case MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
					progressBar.setVisibility(View.VISIBLE);
					stopUpdatingVideoPosition();
					yield true;
				}
				case MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
					progressBar.setVisibility(View.GONE);
					startUpdatingVideoPosition(player);
					yield true;
				}
				default -> false;
			};
		}

		@Override
		public void onSeekComplete(MediaPlayer mp){
			if(getAbsoluteAdapterPosition()==currentIndex && player.isPlaying())
				startUpdatingVideoPosition(player);
		}

		@Override
		public void onCompletion(MediaPlayer mp){
			videoPlayPauseButton.setImageResource(R.drawable.ic_play_24);
			videoPlayPauseButton.setContentDescription(activity.getString(R.string.play));
			stopUpdatingVideoPosition();
			if(!uiVisible)
				toggleUI();
			windowView.removeCallbacks(uiAutoHider);
		}
	}
}
