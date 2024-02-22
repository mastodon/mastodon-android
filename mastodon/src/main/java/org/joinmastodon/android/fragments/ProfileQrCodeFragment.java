package org.joinmastodon.android.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService;
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.googleservices.GmsClient;
import org.joinmastodon.android.googleservices.barcodescanner.Barcode;
import org.joinmastodon.android.googleservices.barcodescanner.BarcodeScanner;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.drawables.FancyQrCodeDrawable;
import org.joinmastodon.android.ui.drawables.RadialParticleSystemDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FixedAspectRatioFrameLayout;
import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.CustomViewHelper;
import me.grishka.appkit.utils.V;

public class ProfileQrCodeFragment extends AppKitFragment{
	private static final String TAG="ProfileQrCodeFragment";
	private static final int PERMISSION_RESULT=388;
	private static final int SCAN_RESULT=439;

	private Context themeWrapper;
	private GradientDrawable scrim=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xE6000000, 0xD9000000});
	private RadialParticleSystemDrawable particles;
	private View codeContainer;
	private View particleAnimContainer;
	private Animator currentTransition;

	private String accountID;
	private Account account;
	private String accountDomain;
	private Intent scannerIntent;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_FRAME, 0);
		setHasOptionsMenu(true);
		accountID=getArguments().getString("account");
		account=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		setCancelable(false);
		scannerIntent=BarcodeScanner.createIntent(Barcode.FORMAT_QR_CODE, false, true);
	}

	@Override
	public void onStart(){
		super.onStart();
		Dialog dlg=getDialog();
		dlg.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
		dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
		dlg.getWindow().setNavigationBarColor(0);
		dlg.getWindow().setStatusBarColor(0);
		WindowManager.LayoutParams lp=dlg.getWindow().getAttributes();
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
			lp.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
		dlg.getWindow().setAttributes(lp);
		if(!isTablet){
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		dlg.setOnKeyListener((dialog, keyCode, event)->{
			if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
				dismiss();
			}
			return true;
		});
	}

	@Override
	public void onDismiss(DialogInterface dialog){
		super.onDismiss(dialog);
		Activity activity=getActivity();
		if(activity!=null)
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		themeWrapper=new ContextThemeWrapper(activity, R.style.Theme_Mastodon_Dark);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View content=View.inflate(themeWrapper, R.layout.fragment_profile_qr, container);
		View decor=getDialog().getWindow().getDecorView();
		decor.setOnApplyWindowInsetsListener((v, insets)->{
			content.setPadding(insets.getStableInsetLeft(), insets.getStableInsetTop(), insets.getStableInsetRight(), insets.getStableInsetBottom());
			return insets.consumeStableInsets();
		});
		int flags=decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		flags&=~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
		decor.setSystemUiVisibility(flags);
		content.setBackground(scrim);

		String url=account.url;
		QRCodeWriter writer=new QRCodeWriter();
		BitMatrix code;
		try{
			code=writer.encode(url, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 0, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H));
		}catch(WriterException e){
			throw new RuntimeException(e);
		}

		View codeView=content.findViewById(R.id.code);
		ImageView avatar=content.findViewById(R.id.avatar);
		TextView username=content.findViewById(R.id.username);
		TextView domain=content.findViewById(R.id.domain);
		View share=content.findViewById(R.id.share_btn);
		Button save=content.findViewById(R.id.save_btn);
		View cornerAnimContainer=content.findViewById(R.id.corner_animation_container);
		particleAnimContainer=content.findViewById(R.id.particle_animation_container);
		codeContainer=content.findViewById(R.id.code_container);

		if(!TextUtils.isEmpty(account.avatar)){
			ViewImageLoader.loadWithoutAnimation(avatar, getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(24), V.dp(24), List.of(), Uri.parse(account.avatarStatic)));
		}
		username.setText(account.username);
		String accDomain=account.getDomain();
		domain.setText(accountDomain=TextUtils.isEmpty(accDomain) ? AccountSessionManager.get(accountID).domain : accDomain);
		Drawable logo=getResources().getDrawable(R.drawable.ic_ntf_logo, themeWrapper.getTheme()).mutate();
		logo.setTint(UiUtils.getThemeColor(themeWrapper, R.attr.colorM3OnPrimary));
		codeView.setBackground(new FancyQrCodeDrawable(code, UiUtils.getThemeColor(themeWrapper, R.attr.colorM3OnPrimary), logo));

		share.setOnClickListener(v->{
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, account.url);
			startActivity(Intent.createChooser(intent, getString(R.string.share_user)));
		});
		save.setOnClickListener(v->saveCodeAsFile());

		cornerAnimContainer.setBackground(new AnimatedCornersDrawable(themeWrapper));
		int particleColor=UiUtils.getThemeColor(themeWrapper, R.attr.colorM3Primary);
		particles=new RadialParticleSystemDrawable(5000, 200, (particleColor & 0xFFFFFF) | 0x80000000, particleColor & 0xFFFFFF, V.dp(65), V.dp(50), getResources().getDisplayMetrics().density);
		particleAnimContainer.setBackground(particles);

		return content;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(savedInstanceState==null){
			AnimatorSet set=new AnimatorSet();
			set.playTogether(
					ObjectAnimator.ofInt(scrim, "alpha", 0, 255),
					ObjectAnimator.ofFloat(particleAnimContainer, View.TRANSLATION_Y, V.dp(50), 0),
					ObjectAnimator.ofFloat(particleAnimContainer, View.ALPHA, 0, 1),
					ObjectAnimator.ofFloat(getToolbar(), View.ALPHA, 0, 1)
			);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.setDuration(350);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					currentTransition=null;
				}
			});
			currentTransition=set;
			set.start();
		}
	}

	@Override
	public void dismiss(){
		dismissWithAnimation(super::dismiss);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(GmsClient.isGooglePlayServicesAvailable(getActivity())){
			MenuItem item=menu.add(0, 0, 0, R.string.scan_qr_code);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			item.setIcon(R.drawable.ic_qr_code_scanner_24px);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(scannerIntent.resolveActivity(getActivity().getPackageManager())!=null){
			startActivityForResult(scannerIntent, SCAN_RESULT);
		}else{
			ProgressDialog progress=new ProgressDialog(getActivity());
			progress.setMessage(getString(R.string.loading));
			progress.setCancelable(false);
			progress.show();
			GmsClient.getModuleInstallerService(getActivity(), new GmsClient.ServiceConnectionCallback<>(){
				@Override
				public void onSuccess(IModuleInstallService service, int connectionID){
					ApiFeatureRequest req=new ApiFeatureRequest();
					req.callingPackage=getActivity().getPackageName();
					Feature feature=new Feature();
					feature.name="mlkit.barcode.ui";
					feature.version=1;
					feature.oldVersion=-1;
					req.features=List.of(feature);
					req.urgent=true;
					try{
						service.installModules(new IModuleInstallCallbacks.Stub(){
							@Override
							public void onModuleAvailabilityResponse(Status status, ModuleAvailabilityResponse response) throws RemoteException{}

							@Override
							public void onModuleInstallResponse(Status status, ModuleInstallResponse response) throws RemoteException{}

							@Override
							public void onModuleInstallIntentResponse(Status status, ModuleInstallIntentResponse response) throws RemoteException{}

							@Override
							public void onStatus(Status status) throws RemoteException{}
						}, req, new IModuleInstallStatusListener.Stub(){
							@Override
							public void onModuleInstallStatusUpdate(ModuleInstallStatusUpdate statusUpdate) throws RemoteException{
								if(statusUpdate.installState==ModuleInstallStatusUpdate.STATE_COMPLETED){
									Runnable r=new Runnable(){
										@Override
										public void run(){
											if(scannerIntent.resolveActivity(getActivity().getPackageManager())!=null){
												progress.dismiss();
												startActivityForResult(scannerIntent, SCAN_RESULT);
											}else{
												codeContainer.postDelayed(this, 100);
											}
										}
									};
									getActivity().runOnUiThread(r);
									GmsClient.disconnectFromService(getActivity(), connectionID);
								}else if(statusUpdate.installState==ModuleInstallStatusUpdate.STATE_FAILED || statusUpdate.installState==ModuleInstallStatusUpdate.STATE_CANCELED){
									getActivity().runOnUiThread(()->{
										progress.dismiss();
										Toast.makeText(themeWrapper, R.string.error, Toast.LENGTH_SHORT).show();
									});
									GmsClient.disconnectFromService(getActivity(), connectionID);
								}
							}
						});
					}catch(RemoteException e){
						Log.e(TAG, "onSuccess: ", e);
						getActivity().runOnUiThread(()->{
							progress.dismiss();
							Toast.makeText(themeWrapper, R.string.error, Toast.LENGTH_SHORT).show();
						});
						GmsClient.disconnectFromService(getActivity(), connectionID);
					}
				}

				@Override
				public void onError(Exception error){
					Log.e(TAG, "onError() called with: error = ["+error+"]");
					Toast.makeText(themeWrapper, R.string.error, Toast.LENGTH_SHORT).show();
					progress.dismiss();
				}
			});
		}
		return true;
	}

	@Override
	protected boolean canGoBack(){
		return true;
	}

	@Override
	public void onToolbarNavigationClick(){
		dismiss();
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
	}

	@Override
	protected int getNavigationIconDrawableResource(){
		return R.drawable.ic_baseline_close_24;
	}

	@Override
	protected LayoutInflater getToolbarLayoutInflater(){
		return LayoutInflater.from(themeWrapper);
	}

	@Override
	protected int getToolbarResource(){
		return R.layout.profile_qr_toolbar;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
		if(requestCode==PERMISSION_RESULT){
			if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
				doSaveCodeAsFile();
			}else if(!getActivity().shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.permission_required)
						.setMessage(R.string.storage_permission_to_download)
						.setPositiveButton(R.string.open_settings, (dialog, which)->getActivity().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getActivity().getPackageName(), null))))
						.setNegativeButton(R.string.cancel, null)
						.show();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==SCAN_RESULT && resultCode==Activity.RESULT_OK && BarcodeScanner.isValidResult(data)){
			Barcode code=BarcodeScanner.getResult(data);
			if(code!=null){
				if(code.rawValue.startsWith("https:")){
					((MainActivity)getActivity()).handleURL(Uri.parse(code.rawValue), accountID);
					dismiss();
				}
			}
		}
	}

	private void dismissWithAnimation(Runnable onDone){
		if(currentTransition!=null)
			currentTransition.cancel();
		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofInt(scrim, "alpha", 0),
				ObjectAnimator.ofFloat(particleAnimContainer, View.TRANSLATION_Y, V.dp(50)),
				ObjectAnimator.ofFloat(particleAnimContainer, View.ALPHA, 0),
				ObjectAnimator.ofFloat(getToolbar(), View.ALPHA, 0)
		);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.setDuration(200);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				onDone.run();
			}
		});
		currentTransition=set;
		set.start();
	}

	private void saveCodeAsFile(){
		if(Build.VERSION.SDK_INT>=29){
			doSaveCodeAsFile();
		}else{
			if(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_RESULT);
			}else{
				doSaveCodeAsFile();
			}
		}
	}

	private void doSaveCodeAsFile(){
		Bitmap bmp=Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
		Canvas c=new Canvas(bmp);
		float factor=1080f/codeContainer.getWidth();
		c.scale(factor, factor);
		codeContainer.draw(c);
		Activity activity=getActivity();
		MastodonAPIController.runInBackground(()->{
			String fileName=account.username+"_"+accountDomain+".png";
			try(OutputStream os=destinationStreamForFile(fileName)){
				bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
				activity.runOnUiThread(()->Toast.makeText(activity, R.string.file_saved, Toast.LENGTH_SHORT).show());
				if(Build.VERSION.SDK_INT<29){
					File dstFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
					MediaScannerConnection.scanFile(activity, new String[]{dstFile.getAbsolutePath()}, new String[]{"image/png"}, null);
				}
			}catch(IOException x){
				activity.runOnUiThread(()->Toast.makeText(activity, R.string.error_saving_file, Toast.LENGTH_SHORT).show());
			}
		});
	}

	private OutputStream destinationStreamForFile(String fileName) throws IOException{
		if(Build.VERSION.SDK_INT>=29){
			ContentValues values=new ContentValues();
			values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
			values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
			values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
			ContentResolver cr=getActivity().getContentResolver();
			Uri itemUri=cr.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
			return cr.openOutputStream(itemUri);
		}else{
			return new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName));
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		codeContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				codeContainer.getViewTreeObserver().removeOnPreDrawListener(this);
				updateParticleEmitter();
				return true;
			}
		});
	}

	private void updateParticleEmitter(){
		int[] loc={0, 0};
		particleAnimContainer.getLocationInWindow(loc);
		int x=loc[0], y=loc[1];
		codeContainer.getLocationInWindow(loc);
		int cx=loc[0]-x+codeContainer.getWidth()/2;
		int cy=loc[1]-y+codeContainer.getHeight()/2;
		int r=codeContainer.getWidth()/2-V.dp(10);
		particles.setEmitterPosition(cx, cy);
		particles.setClipOutBounds(cx-r, cy-r, cx+r, cy+r);
	}

	public static class CustomizedLinearLayout extends LinearLayout implements CustomViewHelper{
		public CustomizedLinearLayout(Context context){
			this(context, null);
		}

		public CustomizedLinearLayout(Context context, AttributeSet attrs){
			this(context, attrs, 0);
		}

		public CustomizedLinearLayout(Context context, AttributeSet attrs, int defStyle){
			super(context, attrs, defStyle);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			int maxW=dp(400);
			FixedAspectRatioFrameLayout aspectLayout=(FixedAspectRatioFrameLayout) getChildAt(0);
			if(MeasureSpec.getSize(widthMeasureSpec)>maxW){
				widthMeasureSpec=MeasureSpec.getMode(widthMeasureSpec) | maxW;
				aspectLayout.setUseHeight(MeasureSpec.getSize(heightMeasureSpec)<dp(464));
			}else{
				aspectLayout.setUseHeight(false);
			}
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	private class AnimatedCornersDrawable extends Drawable{
		private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		private RectF tmpRect=new RectF();

		public AnimatedCornersDrawable(Context context){
			paint.setColor(UiUtils.getThemeColor(context, R.attr.colorM3Primary));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeWidth(V.dp(4));
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			float inset=V.dp(24);
			float radius=V.dp(40);
			float animProgress=((float)Math.sin(Math.toRadians(SystemClock.uptimeMillis()/16.6%360.0))+1f)/2f;
			tmpRect.set(getBounds());
			tmpRect.inset(inset, inset);
			canvas.save();
			float factor=1f+0.025f*animProgress;
			paint.setStrokeWidth(V.dp(4)/factor);
			canvas.scale(factor, factor, tmpRect.centerX(), tmpRect.centerY());
			canvas.drawRoundRect(tmpRect, radius, radius, paint);
			canvas.restore();
			invalidateSelf();
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
			super.onBoundsChange(bounds);
			float inset=V.dp(24);
			float radius=V.dp(40);
			float additionalLength=V.dp(40);
			tmpRect.set(getBounds());
			tmpRect.inset(inset, inset);
			float[] intervals=new float[]{3.1415f*radius*0.5f+additionalLength*2f, tmpRect.width()-radius*2f-additionalLength*2f};
			paint.setPathEffect(new DashPathEffect(intervals, intervals[0]-additionalLength));
			updateParticleEmitter();
		}
	}
}
