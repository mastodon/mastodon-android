package org.joinmastodon.android.ui.photoviewer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.WindowRootFrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class AvatarCropper implements ZoomPanView.Listener{
	private Activity activity;
	private Context context;
	private WindowManager wm;
	private WindowRootFrameLayout windowView;
	private FragmentRootLinearLayout overlay;
	private ZoomPanView zoomPanView;
	private ImageButton closeButton;
	private ImageView image;
	private View confirmButton;
	private Runnable onCancel;
	private OnCropChosenListener cropChosenListener;
	private Uri originalUri;
	private PhotoViewer.Listener listener;
	private Drawable background=new ColorDrawable(0xff000000);

	public AvatarCropper(Activity activity, Uri imageUri, PhotoViewer.Listener photoViewerListener, OnCropChosenListener cropChosenListener, Runnable onCancel){
		this.activity=activity;
		this.context=new ContextThemeWrapper(activity, UiUtils.getThemeForUserPreference(activity, GlobalUserPreferences.ThemePreference.DARK));
		originalUri=imageUri;
		wm=context.getSystemService(WindowManager.class);
		this.cropChosenListener=cropChosenListener;
		this.onCancel=onCancel;
		this.listener=photoViewerListener;

		windowView=(WindowRootFrameLayout) LayoutInflater.from(this.context).inflate(R.layout.avatar_cropper, null);
		overlay=windowView.findViewById(R.id.overlay);
		closeButton=windowView.findViewById(R.id.btn_back);
		zoomPanView=windowView.findViewById(R.id.zoom_pan_view);
		image=windowView.findViewById(R.id.image);
		confirmButton=windowView.findViewById(R.id.btn_confirm);

		windowView.setBackground(background);
		windowView.setDispatchApplyWindowInsetsListener((v, insets)->{
			int bottomInset=0;
			if(Build.VERSION.SDK_INT>=27){
				int inset=insets.getSystemWindowInsetBottom();
				bottomInset=inset>0 ? Math.max(inset, V.dp(24)) : 0;
			}
			((FrameLayout.LayoutParams)confirmButton.getLayoutParams()).bottomMargin=bottomInset+V.dp(16+80);
			return overlay.dispatchApplyWindowInsets(insets);
		});
		windowView.setDispatchKeyEventListener((v, keyCode, event)->{
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU && event.getKeyCode()==KeyEvent.KEYCODE_BACK){
				if(event.getAction()==KeyEvent.ACTION_DOWN){
					dismiss(true, onCancel);
				}
				return true;
			}
			return false;
		});
		closeButton.setOnClickListener(v->dismiss(true, onCancel));
		overlay.setStatusBarColor(0);
		overlay.setNavigationBarColor(0);
		overlay.setBackground(new OverlayDrawable());
		zoomPanView.setListener(this);
		zoomPanView.setFill(true);
		zoomPanView.setSwipeToDismissEnabled(false);
		ViewImageLoader.load(new ViewImageLoader.Target(){
			@Override
			public void setImageDrawable(Drawable d){
				if(d!=null){
					image.setImageDrawable(d);
					image.setLayoutParams(new FrameLayout.LayoutParams(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Gravity.CENTER));
					zoomPanView.updateLayout();
				}
			}

			@Override
			public View getView(){
				return image;
			}
		}, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, 0, 0, List.of(), imageUri), false);
		windowView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)->{
			if(left==oldLeft && top==oldTop && right==oldRight && bottom==oldBottom)
				return;
			int width=right-left;
			int height=bottom-top;
			int size=V.dp(192);
			int hpad=(width-size)/2;
			int vpad=(height-size)/2;
			zoomPanView.setPadding(hpad, vpad, hpad, vpad);
			zoomPanView.updateLayout();
		});
		confirmButton.setOnClickListener(v->confirm());
	}

	public void show(){
		WindowManager.LayoutParams wlp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
		wlp.type=WindowManager.LayoutParams.TYPE_APPLICATION;
		wlp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
				| WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
		wlp.format=PixelFormat.TRANSLUCENT;
		wlp.setTitle(context.getString(R.string.avatar_move_and_scale));
		if(Build.VERSION.SDK_INT>=28)
			wlp.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30 ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		windowView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		wm.addView(windowView, wlp);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
			windowView.findOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, ()->dismiss(true, onCancel));
		}
	}

	public void dismiss(boolean animated, Runnable onDone){
		if(animated){
			windowView.animate()
					.alpha(0)
					.setDuration(250)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
					.withEndAction(()->{
						wm.removeView(windowView);
						if(onDone!=null)
							onDone.run();
					})
					.start();
		}else{
			wm.removeView(windowView);
			if(onDone!=null)
				onDone.run();
		}
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
		background.setAlpha(Math.round(255*alpha));
		overlay.setAlpha(alpha);
		confirmButton.setAlpha(alpha);
	}

	@Override
	public void onStartSwipeToDismiss(){

	}

	@Override
	public void onStartSwipeToDismissTransition(float velocityY){

	}

	@Override
	public void onSwipeToDismissCanceled(){

	}

	@Override
	public void onDismissed(){
		listener.setPhotoViewVisibility(0, true);
		wm.removeView(windowView);
		listener.photoViewerDismissed();
	}

	@Override
	public void onSingleTap(){

	}

	private void confirm(){
		// stop receiving input events to allow the user to interact with the underlying UI while the animation is still running
		WindowManager.LayoutParams wlp=(WindowManager.LayoutParams) windowView.getLayoutParams();
		wlp.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		windowView.setSystemUiVisibility(windowView.getSystemUiVisibility() | (activity.getWindow().getDecorView().getSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)));
		wm.updateViewLayout(windowView, wlp);

		Drawable drawable=image.getDrawable();
		zoomPanView.endAllAnimations();
		Rect rect=new Rect();
		image.getHitRect(rect);
		float scale=image.getScaleX();
		int x=Math.round((zoomPanView.getPaddingLeft()-rect.left)/scale);
		int y=Math.round((zoomPanView.getPaddingTop()-rect.top)/scale);
		int size=Math.round(V.dp(192)/scale);
		if(x==0 && y==0 && drawable.getIntrinsicWidth()==drawable.getIntrinsicHeight() && size==drawable.getIntrinsicWidth()){
			dismissWithTransition();
			cropChosenListener.onCropChosen(drawable, originalUri);
			return;
		}

		Bitmap croppedBitmap=Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		Canvas c=new Canvas(croppedBitmap);
		c.translate(-x, -y);
		drawable.draw(c);

		MastodonAPIController.runInBackground(()->{
			String mimetype;
			if("file".equals(originalUri.getScheme())){
				mimetype=UiUtils.getFileMediaType(new File(originalUri.getPath())).type();
			}else{
				mimetype=activity.getContentResolver().getType(originalUri);
			}
			if(mimetype==null)
				mimetype="image/jpeg";
			Bitmap.CompressFormat format=switch(mimetype){
				case "image/png", "image/gif" -> Bitmap.CompressFormat.PNG;
				default -> Bitmap.CompressFormat.JPEG;
			};
			File outputFile=new File(activity.getCacheDir(), "avatar_upload."+(format==Bitmap.CompressFormat.PNG ? "png" : "jpg"));
			try(FileOutputStream out=new FileOutputStream(outputFile)){
				croppedBitmap.compress(format, 97, out);
			}catch(IOException e){
				activity.runOnUiThread(()->{
					Toast.makeText(activity, R.string.error_saving_file, Toast.LENGTH_SHORT).show();
					dismiss(true, onCancel);
				});
				return;
			}
			outputFile.deleteOnExit();
			activity.runOnUiThread(()->{
				image.setImageBitmap(croppedBitmap);
				image.getLayoutParams().width=image.getLayoutParams().height=size;
				zoomPanView.updateLayout();
				cropChosenListener.onCropChosen(new BitmapDrawable(croppedBitmap), Uri.fromFile(outputFile));
				dismissWithTransition();
			});
		});
	}

	private void dismissWithTransition(){
		zoomPanView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				zoomPanView.getViewTreeObserver().removeOnPreDrawListener(this);

				listener.setPhotoViewVisibility(0, true);
				int[] radius=new int[4];
				Rect rect=new Rect();
				if(listener.startPhotoViewTransition(0, rect, radius)){
					zoomPanView.animateOut(rect, radius, 0);
				}else{
					windowView.animate()
							.alpha(0)
							.setDuration(300)
							.setInterpolator(CubicBezierInterpolator.DEFAULT)
							.withEndAction(AvatarCropper.this::onDismissed)
							.start();
				}

				return true;
			}
		});
	}

	private static class OverlayDrawable extends Drawable{
		private Path path=new Path(), tmpPath=new Path();
		private Paint overlayPaint=new Paint(Paint.ANTI_ALIAS_FLAG), strokePaint=new Paint(Paint.ANTI_ALIAS_FLAG);

		public OverlayDrawable(){
			overlayPaint.setColor(0xb3000000);
			strokePaint.setColor(0x4dffffff);
			strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(V.dp(1));
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			canvas.drawPath(path, overlayPaint);

			Rect bounds=getBounds();
			float size=V.dp(192)-strokePaint.getStrokeWidth();
			float x=bounds.centerX()-size/2;
			float y=bounds.centerY()-size/2;
			float radius=V.dp(40)-strokePaint.getStrokeWidth()/2f;
			canvas.drawRoundRect(x, y, x+size, y+size, radius, radius, strokePaint);
		}

		@Override
		public void setAlpha(int alpha){

		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){

		}

		@Override
		public int getOpacity(){
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		protected void onBoundsChange(@NonNull Rect bounds){
			path.rewind();
			path.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
			tmpPath.rewind();
			int size=V.dp(192);
			int x=bounds.centerX()-size/2;
			int y=bounds.centerY()-size/2;
			tmpPath.addRoundRect(x, y, x+size, y+size, V.dp(40), V.dp(40), Path.Direction.CW);
			path.op(tmpPath, Path.Op.DIFFERENCE);
		}
	}

	public interface OnCropChosenListener{
		void onCropChosen(Drawable thumbnail, Uri uri);
	}
}
