package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FixedAspectRatioImageView;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeImageDescriptionFragment extends MastodonToolbarFragment implements OnBackPressedListener{
	private static final String TAG="ComposeImageDescription";

	private String accountID, attachmentID;
	private EditText edit;
	private FixedAspectRatioImageView image;
	private ContextThemeWrapper themeWrapper;
	private PhotoViewer photoViewer;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		attachmentID=getArguments().getString("attachment");
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		themeWrapper=new ContextThemeWrapper(activity, R.style.Theme_Mastodon_Dark);
		setTitle(R.string.add_alt_text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return super.onCreateView(themeWrapper.getSystemService(LayoutInflater.class), container, savedInstanceState);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_image_description, container, false);

		edit=view.findViewById(R.id.edit);
		image=view.findViewById(R.id.photo);
		int width=getArguments().getInt("width", 0);
		int height=getArguments().getInt("height", 0);
		if(width>0 && height>0){
			image.setAspectRatio(Math.max(1f, (float)width/height));
		}
		image.setOnClickListener(v->openPhotoViewer());
		Uri uri=getArguments().getParcelable("uri");
		Attachment.Type type=Attachment.Type.valueOf(getArguments().getString("attachmentType"));
		if(type==Attachment.Type.IMAGE)
			ViewImageLoader.load(image, null, new UrlImageLoaderRequest(uri, 1000, 1000));
		else
			loadVideoThumbIntoView(image, uri);
		edit.setText(getArguments().getString("existingDescription"));

		return view;
	}

	private void loadVideoThumbIntoView(ImageView target, Uri uri){
		MastodonAPIController.runInBackground(()->{
			Context context=getActivity();
			if(context==null)
				return;
			try{
				MediaMetadataRetriever mmr=new MediaMetadataRetriever();
				mmr.setDataSource(context, uri);
				Bitmap frame=mmr.getFrameAtTime(3_000_000);
				mmr.release();
				int size=Math.max(frame.getWidth(), frame.getHeight());
				int maxSize=V.dp(250);
				if(size>maxSize){
					float factor=maxSize/(float)size;
					frame=Bitmap.createScaledBitmap(frame, Math.round(frame.getWidth()*factor), Math.round(frame.getHeight()*factor), true);
				}
				Bitmap finalFrame=frame;
				target.post(()->target.setImageBitmap(finalFrame));
			}catch(Exception x){
				Log.w(TAG, "loadVideoThumbIntoView: error getting video frame", x);
			}
		});
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		edit.requestFocus();
		view.postDelayed(()->getActivity().getSystemService(InputMethodManager.class).showSoftInput(edit, 0), 100);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.compose_image_description, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==R.id.help){
			SpannableStringBuilder msg=new SpannableStringBuilder(getText(R.string.alt_text_help));
			BulletSpan[] spans=msg.getSpans(0, msg.length(), BulletSpan.class);
			for(BulletSpan span:spans){
				BulletSpan betterSpan;
				if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
					betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(themeWrapper, R.attr.colorM3OnSurface));
				else
					betterSpan=new BulletSpan(V.dp(10), UiUtils.getThemeColor(themeWrapper, R.attr.colorM3OnSurface), V.dp(1.5f));
				msg.setSpan(betterSpan, msg.getSpanStart(span), msg.getSpanEnd(span), msg.getSpanFlags(span));
				msg.removeSpan(span);
			}
			new M3AlertDialogBuilder(themeWrapper)
					.setTitle(R.string.what_is_alt_text)
					.setMessage(msg)
					.setPositiveButton(R.string.ok, null)
					.show();
		}
		return true;
	}

	@Override
	public boolean onBackPressed(){
		deliverResult();
		return false;
	}

	@Override
	protected LayoutInflater getToolbarLayoutInflater(){
		return LayoutInflater.from(themeWrapper);
	}

	private void deliverResult(){
		Bundle r=new Bundle();
		r.putString("text", edit.getText().toString().trim());
		r.putString("attachment", attachmentID);
		setResult(true, r);
	}

	private void openPhotoViewer(){
		Attachment fakeAttachment=new Attachment();
		fakeAttachment.id="local";
		fakeAttachment.type=Attachment.Type.valueOf(getArguments().getString("attachmentType"));
		int width=getArguments().getInt("width", 0);
		int height=getArguments().getInt("height", 0);
		Uri uri=getArguments().getParcelable("uri");
		fakeAttachment.url=uri.toString();
		fakeAttachment.meta=new Attachment.Metadata();
		fakeAttachment.meta.width=width;
		fakeAttachment.meta.height=height;

		photoViewer=new PhotoViewer(getActivity(), Collections.singletonList(fakeAttachment), 0, new PhotoViewer.Listener(){
			@Override
			public void setPhotoViewVisibility(int index, boolean visible){
				image.setAlpha(visible ? 1f : 0f);
			}

			@Override
			public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
				int[] pos={0, 0};
				image.getLocationOnScreen(pos);
				outRect.set(pos[0], pos[1], pos[0]+image.getWidth(), pos[1]+image.getHeight());
				image.setElevation(1f);
				return true;
			}

			@Override
			public void setTransitioningViewTransform(float translateX, float translateY, float scale){
				image.setTranslationX(translateX);
				image.setTranslationY(translateY);
				image.setScaleX(scale);
				image.setScaleY(scale);
			}

			@Override
			public void endPhotoViewTransition(){
				Drawable d=image.getDrawable();
				image.setImageDrawable(null);
				image.setImageDrawable(d);

				image.setTranslationX(0f);
				image.setTranslationY(0f);
				image.setScaleX(1f);
				image.setScaleY(1f);
				image.setElevation(0f);
			}

			@Nullable
			@Override
			public Drawable getPhotoViewCurrentDrawable(int index){
				return image.getDrawable();
			}

			@Override
			public void photoViewerDismissed(){
				photoViewer=null;
			}

			@Override
			public void onRequestPermissions(String[] permissions){

			}
		});
		photoViewer.removeMenu();
	}
}
