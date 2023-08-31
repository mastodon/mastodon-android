package org.joinmastodon.android.ui.viewcontrollers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.ProgressListener;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.GetAttachmentByID;
import org.joinmastodon.android.api.requests.statuses.UpdateAttachment;
import org.joinmastodon.android.api.requests.statuses.UploadAttachment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ComposeImageDescriptionFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.utils.TransferSpeedTracker;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ComposeMediaViewController{
	private static final int MAX_ATTACHMENTS=4;
	private static final String TAG="ComposeMediaViewControl";
	
	private final ComposeFragment fragment;
	
	private ReorderableLinearLayout attachmentsView;
	private HorizontalScrollView attachmentsScroller;
	
	private ArrayList<DraftMediaAttachment> attachments=new ArrayList<>();
	private boolean attachmentsErrorShowing;

	public ComposeMediaViewController(ComposeFragment fragment){
		this.fragment=fragment;
	}

	public void setView(View view, Bundle savedInstanceState){
		attachmentsView=view.findViewById(R.id.attachments);
		attachmentsScroller=view.findViewById(R.id.attachments_scroller);
		attachmentsView.setDividerDrawable(new EmptyDrawable(V.dp(8), 0));
		attachmentsView.setDragListener(new AttachmentDragListener());
		attachmentsView.setMoveInBothDimensions(true);

		if(!fragment.getWasDetached() && savedInstanceState!=null && savedInstanceState.containsKey("attachments")){
			ArrayList<Parcelable> serializedAttachments=savedInstanceState.getParcelableArrayList("attachments");
			for(Parcelable a:serializedAttachments){
				DraftMediaAttachment att=Parcels.unwrap(a);
				attachmentsView.addView(createMediaAttachmentView(att));
				attachments.add(att);
			}
			attachmentsScroller.setVisibility(View.VISIBLE);
			updateMediaAttachmentsLayout();
		}else if(!attachments.isEmpty()){
			attachmentsScroller.setVisibility(View.VISIBLE);
			for(DraftMediaAttachment att:attachments){
				attachmentsView.addView(createMediaAttachmentView(att));
			}
			updateMediaAttachmentsLayout();
		}
	}

	public void onViewCreated(Bundle savedInstanceState){
		if(savedInstanceState==null && !fragment.editingStatus.mediaAttachments.isEmpty()){
			attachmentsScroller.setVisibility(View.VISIBLE);
			for(Attachment att:fragment.editingStatus.mediaAttachments){
				DraftMediaAttachment da=new DraftMediaAttachment();
				da.serverAttachment=att;
				da.description=att.description;
				da.uri=att.previewUrl!=null ? Uri.parse(att.previewUrl) : null;
				da.state=AttachmentUploadState.DONE;
				attachmentsView.addView(createMediaAttachmentView(da));
				attachments.add(da);
			}
			updateMediaAttachmentsLayout();
		}
	}
	
	public boolean addMediaAttachment(Uri uri, String description){
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS){
			showMediaAttachmentError(fragment.getResources().getQuantityString(R.plurals.cant_add_more_than_x_attachments, MAX_ATTACHMENTS, MAX_ATTACHMENTS));
			return false;
		}
		String type=fragment.getActivity().getContentResolver().getType(uri);
		int size;
		try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
			cursor.moveToFirst();
			size=cursor.getInt(0);
		}catch(Exception x){
			Log.w("ComposeFragment", x);
			return false;
		}
		Instance instance=fragment.instance;
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null){
			if(instance.configuration.mediaAttachments.supportedMimeTypes!=null && !instance.configuration.mediaAttachments.supportedMimeTypes.contains(type)){
				showMediaAttachmentError(fragment.getString(R.string.media_attachment_unsupported_type, UiUtils.getFileName(uri)));
				return false;
			}
			if(!type.startsWith("image/")){
				int sizeLimit=instance.configuration.mediaAttachments.videoSizeLimit;
				if(size>sizeLimit){
					float mb=sizeLimit/(float) (1024*1024);
					String sMb=String.format(Locale.getDefault(), mb%1f==0f ? "%.0f" : "%.2f", mb);
					showMediaAttachmentError(fragment.getString(R.string.media_attachment_too_big, UiUtils.getFileName(uri), sMb));
					return false;
				}
			}
		}
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.mimeType=type;
		draft.description=description;
		draft.fileSize=size;

		UiUtils.beginLayoutTransition(attachmentsScroller);
		attachmentsView.addView(createMediaAttachmentView(draft), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
		attachments.add(draft);
		attachmentsScroller.setVisibility(View.VISIBLE);
		updateMediaAttachmentsLayout();
//		draft.setOverlayVisible(true, false);

		if(!areThereAnyUploadingAttachments()){
			uploadNextQueuedAttachment();
		}
		fragment.updatePublishButtonState();
		fragment.updateMediaPollStates();
		return true;
	}

	private void updateMediaAttachmentsLayout(){
		int newWidth=attachments.size()>2 ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
		if(newWidth!=attachmentsView.getLayoutParams().width){
			attachmentsView.getLayoutParams().width=newWidth;
			attachmentsScroller.requestLayout();
		}
		for(DraftMediaAttachment att:attachments){
			LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams) att.view.getLayoutParams();
			if(attachments.size()<3){
				lp.width=0;
				lp.weight=1f;
			}else{
				lp.width=V.dp(200);
				lp.weight=0f;
			}
		}
	}

	private void showMediaAttachmentError(String text){
		if(!attachmentsErrorShowing){
			Toast.makeText(fragment.getActivity(), text, Toast.LENGTH_SHORT).show();
			attachmentsErrorShowing=true;
			attachmentsView.postDelayed(()->attachmentsErrorShowing=false, 2000);
		}
	}

	private View createMediaAttachmentView(DraftMediaAttachment draft){
		View thumb=fragment.getActivity().getLayoutInflater().inflate(R.layout.compose_media_thumb, attachmentsView, false);
		ImageView img=thumb.findViewById(R.id.thumb);
		if(draft.serverAttachment!=null){
			if(draft.serverAttachment.previewUrl!=null)
				ViewImageLoader.load(img, draft.serverAttachment.blurhashPlaceholder, new UrlImageLoaderRequest(draft.serverAttachment.previewUrl, V.dp(250), V.dp(250)));
		}else{
			if(draft.mimeType.startsWith("image/")){
				ViewImageLoader.load(img, null, new UrlImageLoaderRequest(draft.uri, V.dp(250), V.dp(250)));
			}else if(draft.mimeType.startsWith("video/")){
				loadVideoThumbIntoView(img, draft.uri);
			}
		}

		draft.view=thumb;
		draft.imageView=img;
		draft.progressBar=thumb.findViewById(R.id.progress);
		draft.titleView=thumb.findViewById(R.id.title);
		draft.subtitleView=thumb.findViewById(R.id.subtitle);
		draft.removeButton=thumb.findViewById(R.id.delete);
		draft.editButton=thumb.findViewById(R.id.edit);
		draft.dragLayer=thumb.findViewById(R.id.drag_layer);

		draft.removeButton.setTag(draft);
		draft.removeButton.setOnClickListener(this::onRemoveMediaAttachmentClick);
		draft.editButton.setTag(draft);

		thumb.setOutlineProvider(OutlineProviders.roundedRect(12));
		thumb.setClipToOutline(true);
		img.setOutlineProvider(OutlineProviders.roundedRect(12));
		img.setClipToOutline(true);

		thumb.setBackgroundColor(UiUtils.getThemeColor(fragment.getActivity(), R.attr.colorM3Surface));
		thumb.setOnLongClickListener(v->{
			if(!v.hasTransientState() && attachments.size()>1){
				attachmentsView.startDragging(v);
				return true;
			}
			return false;
		});
		thumb.setTag(draft);


		if(draft.fileSize>0){
			int subtitleRes=switch(Objects.requireNonNullElse(draft.mimeType, "").split("/")[0]){
				case "image" -> R.string.attachment_description_image;
				case "video" -> R.string.attachment_description_video;
				case "audio" -> R.string.attachment_description_audio;
				default -> R.string.attachment_description_unknown;
			};
			draft.subtitleView.setText(fragment.getString(subtitleRes, UiUtils.formatFileSize(fragment.getActivity(), draft.fileSize, true)));
		}else if(draft.serverAttachment!=null){
			int subtitleRes=switch(draft.serverAttachment.type){
				case IMAGE -> R.string.attachment_type_image;
				case VIDEO -> R.string.attachment_type_video;
				case GIFV -> R.string.attachment_type_gif;
				case AUDIO -> R.string.attachment_type_audio;
				case UNKNOWN -> R.string.attachment_type_unknown;
			};
			draft.subtitleView.setText(subtitleRes);
		}
		draft.titleView.setText(fragment.getString(R.string.attachment_x_percent_uploaded, 0));
		draft.removeButton.setImageResource(R.drawable.ic_baseline_close_24);

		if(draft.state==AttachmentUploadState.ERROR){
			draft.titleView.setText(R.string.upload_failed);
			draft.editButton.setImageResource(R.drawable.ic_restart_alt_24px);
			draft.editButton.setOnClickListener(this::onRetryOrCancelMediaUploadClick);
			draft.progressBar.setVisibility(View.GONE);
			draft.setUseErrorColors(true);
		}else if(draft.state==AttachmentUploadState.DONE){
			draft.setDescriptionToTitle();
			draft.progressBar.setVisibility(View.GONE);
			draft.editButton.setOnClickListener(this::onEditMediaDescriptionClick);
		}else{
			draft.editButton.setVisibility(View.GONE);
			draft.removeButton.setImageResource(R.drawable.ic_baseline_close_24);
			if(draft.state==AttachmentUploadState.PROCESSING){
				draft.titleView.setText(R.string.upload_processing);
			}else{
				draft.titleView.setText(fragment.getString(R.string.attachment_x_percent_uploaded, 0));
			}
		}

		return thumb;
	}

	public void addFakeMediaAttachment(Uri uri, String description){
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.description=description;
		draft.mimeType="image/jpeg";
		draft.fileSize=2473276;
		draft.state=AttachmentUploadState.PROCESSING;
		attachmentsView.addView(createMediaAttachmentView(draft));
		attachments.add(draft);
		attachmentsScroller.setVisibility(View.VISIBLE);
		updateMediaAttachmentsLayout();
		finishMediaAttachmentUpload(draft);
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(areThereAnyUploadingAttachments()){
			throw new IllegalStateException("there is already an attachment being uploaded");
		}
		attachment.state=AttachmentUploadState.UPLOADING;
		attachment.progressBar.setVisibility(View.VISIBLE);
		int maxSize=0;
		String contentType=fragment.getActivity().getContentResolver().getType(attachment.uri);
		if(contentType!=null && contentType.startsWith("image/")){
			maxSize=2_073_600; // TODO get this from instance configuration when it gets added there
		}
		attachment.progressBar.setProgress(0);
		attachment.speedTracker.reset();
		attachment.speedTracker.addSample(0);
		attachment.uploadRequest=(UploadAttachment) new UploadAttachment(attachment.uri, maxSize, attachment.description)
				.setProgressListener(new ProgressListener(){
					@Override
					public void onProgress(long transferred, long total){
						if(fragment.getActivity()==null)
							return;
						float progressFraction=transferred/(float)total;
						int progress=Math.round(progressFraction*attachment.progressBar.getMax());
						if(Build.VERSION.SDK_INT>=24)
							attachment.progressBar.setProgress(progress, true);
						else
							attachment.progressBar.setProgress(progress);

						attachment.titleView.setText(fragment.getString(R.string.attachment_x_percent_uploaded, Math.round(progressFraction*100f)));

						attachment.speedTracker.setTotalBytes(total);
//						attachment.uploadStateTitle.setText(fragment.getString(R.string.file_upload_progress, UiUtils.formatFileSize(fragment.getActivity(), transferred, true), UiUtils.formatFileSize(fragment.getActivity(), total, true)));
						attachment.speedTracker.addSample(transferred);
					}
				})
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.serverAttachment=result;
						if(TextUtils.isEmpty(result.url)){
							attachment.state=AttachmentUploadState.PROCESSING;
							attachment.processingPollingRunnable=()->pollForMediaAttachmentProcessing(attachment);
							if(fragment.getActivity()==null)
								return;
							attachment.titleView.setText(R.string.upload_processing);
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
							if(!areThereAnyUploadingAttachments())
								uploadNextQueuedAttachment();
						}else{
							finishMediaAttachmentUpload(attachment);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.uploadRequest=null;
						attachment.state=AttachmentUploadState.ERROR;
						attachment.titleView.setText(R.string.upload_failed);
//						if(error instanceof MastodonErrorResponse er){
//							if(er.underlyingException instanceof SocketException || er.underlyingException instanceof UnknownHostException || er.underlyingException instanceof InterruptedIOException)
//								attachment.uploadStateText.setText(R.string.upload_error_connection_lost);
//							else
//								attachment.uploadStateText.setText(er.error);
//						}else{
//							attachment.uploadStateText.setText("");
//						}
//						attachment.retryButton.setImageResource(R.drawable.ic_fluent_arrow_clockwise_24_filled);
//						attachment.retryButton.setContentDescription(fragment.getString(R.string.retry_upload));

						V.setVisibilityAnimated(attachment.editButton, View.VISIBLE);
						attachment.editButton.setImageResource(R.drawable.ic_restart_alt_24px);
						attachment.editButton.setOnClickListener(ComposeMediaViewController.this::onRetryOrCancelMediaUploadClick);
						attachment.setUseErrorColors(true);
						V.setVisibilityAnimated(attachment.progressBar, View.GONE);

						if(!areThereAnyUploadingAttachments())
							uploadNextQueuedAttachment();
					}
				})
				.exec(fragment.getAccountID());
	}

	private void onRemoveMediaAttachmentClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.isUploadingOrProcessing())
			att.cancelUpload();
		attachments.remove(att);
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		if(!attachments.isEmpty())
			UiUtils.beginLayoutTransition(attachmentsScroller);
		attachmentsView.removeView(att.view);
		if(getMediaAttachmentsCount()==0){
			attachmentsScroller.setVisibility(View.GONE);
		}else{
			updateMediaAttachmentsLayout();
		}
		fragment.updatePublishButtonState();
		fragment.updateMediaPollStates();
	}

	private void onRetryOrCancelMediaUploadClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.state==AttachmentUploadState.ERROR){
//			att.retryButton.setImageResource(R.drawable.ic_fluent_dismiss_24_filled);
//			att.retryButton.setContentDescription(fragment.getString(R.string.cancel));
			V.setVisibilityAnimated(att.progressBar, View.VISIBLE);
			V.setVisibilityAnimated(att.editButton, View.GONE);
			att.titleView.setText(fragment.getString(R.string.attachment_x_percent_uploaded, 0));
			att.state=AttachmentUploadState.QUEUED;
			att.setUseErrorColors(false);
			if(!areThereAnyUploadingAttachments()){
				uploadNextQueuedAttachment();
			}
		}else{
			onRemoveMediaAttachmentClick(v);
		}
	}

	private void loadVideoThumbIntoView(ImageView target, Uri uri){
		MastodonAPIController.runInBackground(()->{
			Context context=fragment.getActivity();
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

	private void pollForMediaAttachmentProcessing(DraftMediaAttachment attachment){
		attachment.processingPollingRequest=(GetAttachmentByID) new GetAttachmentByID(attachment.serverAttachment.id)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.processingPollingRequest=null;
						if(!TextUtils.isEmpty(result.url)){
							attachment.processingPollingRunnable=null;
							attachment.serverAttachment=result;
							finishMediaAttachmentUpload(attachment);
						}else if(fragment.getActivity()!=null){
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.processingPollingRequest=null;
						if(fragment.getActivity()!=null)
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
					}
				})
				.exec(fragment.getAccountID());
	}

	private void finishMediaAttachmentUpload(DraftMediaAttachment attachment){
		if(attachment.state!=AttachmentUploadState.PROCESSING && attachment.state!=AttachmentUploadState.UPLOADING)
			throw new IllegalStateException("Unexpected state "+attachment.state);
		attachment.uploadRequest=null;
		attachment.state=AttachmentUploadState.DONE;
		attachment.editButton.setImageResource(R.drawable.ic_edit_24px);
		attachment.removeButton.setImageResource(R.drawable.ic_delete_24px);
		attachment.editButton.setOnClickListener(this::onEditMediaDescriptionClick);
		V.setVisibilityAnimated(attachment.progressBar, View.GONE);
		V.setVisibilityAnimated(attachment.editButton, View.VISIBLE);
		attachment.setDescriptionToTitle();
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		fragment.updatePublishButtonState();
	}

	private void uploadNextQueuedAttachment(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.QUEUED){
				uploadMediaAttachment(att);
				return;
			}
		}
	}

	public boolean areThereAnyUploadingAttachments(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.UPLOADING)
				return true;
		}
		return false;
	}

	private void onEditMediaDescriptionClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.serverAttachment==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", fragment.getAccountID());
		args.putString("attachment", att.serverAttachment.id);
		args.putParcelable("uri", att.uri);
		args.putString("existingDescription", att.description);
		args.putString("attachmentType", att.serverAttachment.type.toString());
		Drawable img=att.imageView.getDrawable();
		if(img!=null){
			args.putInt("width", img.getIntrinsicWidth());
			args.putInt("height", img.getIntrinsicHeight());
		}
		Nav.goForResult(fragment.getActivity(), ComposeImageDescriptionFragment.class, args, ComposeFragment.IMAGE_DESCRIPTION_RESULT, fragment);
	}

	public int getMediaAttachmentsCount(){
		return attachments.size();
	}
	
	public void cancelAllUploads(){
		for(DraftMediaAttachment att:attachments){
			if(att.isUploadingOrProcessing())
				att.cancelUpload();
		}
	}
	
	public void setAltTextByID(String attID, String text){
		for(DraftMediaAttachment att:attachments){
			if(att.serverAttachment.id.equals(attID)){
				att.descriptionSaved=false;
				att.description=text;
				att.setDescriptionToTitle();
				break;
			}
		}
	}
	
	public List<String> getAttachmentIDs(){
		return attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList());
	}

	public List<CreateStatus.Request.MediaAttribute> getAttachmentAttributes(){
		List<CreateStatus.Request.MediaAttribute> mediaAttributes = new ArrayList<>();
		for (DraftMediaAttachment att:attachments){
			 mediaAttributes.add(new CreateStatus.Request.MediaAttribute(att.serverAttachment.id, att.description, null));
		}
		return mediaAttributes;
	}
	
	public boolean isEmpty(){
		return attachments.isEmpty();
	}

	public boolean canAddMoreAttachments(){
		return attachments.size()<MAX_ATTACHMENTS;
	}

	public int getMissingAltTextAttachmentCount(){
		int count=0;
		for(DraftMediaAttachment att:attachments){
			if(TextUtils.isEmpty(att.description))
				count++;
		}
		return count;
	}

	public boolean areAllAttachmentsImages(){
		for(DraftMediaAttachment att:attachments){
			if((att.mimeType==null && att.serverAttachment.type==Attachment.Type.IMAGE) || (att.mimeType!=null && !att.mimeType.startsWith("image/")))
				return false;
		}
		return true;
	}

	public int getMaxAttachments(){
		return MAX_ATTACHMENTS;
	}

	public int getNonDoneAttachmentCount(){
		int nonDoneAttachmentCount=0;
		for(DraftMediaAttachment att:attachments){
			if(att.state!=AttachmentUploadState.DONE)
				nonDoneAttachmentCount++;
		}
		return nonDoneAttachmentCount;
	}

	public void saveAltTextsBeforePublishing(Runnable onSuccess, Consumer<ErrorResponse> onError){
		ArrayList<UpdateAttachment> updateAltTextRequests=new ArrayList<>();
		for(DraftMediaAttachment att:attachments){
			if(!att.descriptionSaved && att.serverAttachment.description == null){
				UpdateAttachment req=new UpdateAttachment(att.serverAttachment.id, att.description);
				req.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Attachment result){
								att.descriptionSaved=true;
								att.serverAttachment=result;
								updateAltTextRequests.remove(req);
								if(updateAltTextRequests.isEmpty())
									onSuccess.run();
							}

							@Override
							public void onError(ErrorResponse error){
								onError.accept(error);
							}
						})
						.exec(fragment.getAccountID());
				updateAltTextRequests.add(req);
			}
		}
		if(updateAltTextRequests.isEmpty())
			onSuccess.run();
	}

	public void onSaveInstanceState(Bundle outState){
		if(!attachments.isEmpty()){
			ArrayList<Parcelable> serializedAttachments=new ArrayList<>(attachments.size());
			for(DraftMediaAttachment att:attachments){
				serializedAttachments.add(Parcels.wrap(att));
			}
			outState.putParcelableArrayList("attachments", serializedAttachments);
		}
	}

	@Parcel
	static class DraftMediaAttachment{
		public Attachment serverAttachment;
		public Uri uri;
		public transient UploadAttachment uploadRequest;
		public transient GetAttachmentByID processingPollingRequest;
		public String description;
		public String mimeType;
		public AttachmentUploadState state=AttachmentUploadState.QUEUED;
		public int fileSize;
		public boolean descriptionSaved=true;

		public transient View view;
		public transient ProgressBar progressBar;
		public transient ImageButton removeButton, editButton;
		public transient Runnable processingPollingRunnable;
		public transient ImageView imageView;
		public transient TextView titleView, subtitleView;
		public transient TransferSpeedTracker speedTracker=new TransferSpeedTracker();
		private transient boolean errorColors;
		private transient Animator errorTransitionAnimator;
		public transient View dragLayer;

		public void cancelUpload(){
			switch(state){
				case UPLOADING -> {
					if(uploadRequest!=null){
						uploadRequest.cancel();
						uploadRequest=null;
					}
				}
				case PROCESSING -> {
					if(processingPollingRunnable!=null){
						UiUtils.removeCallbacks(processingPollingRunnable);
						processingPollingRunnable=null;
					}
					if(processingPollingRequest!=null){
						processingPollingRequest.cancel();
						processingPollingRequest=null;
					}
				}
				default -> throw new IllegalStateException("Unexpected state "+state);
			}
		}

		public boolean isUploadingOrProcessing(){
			return state==AttachmentUploadState.UPLOADING || state==AttachmentUploadState.PROCESSING;
		}

		public void setDescriptionToTitle(){
			if(TextUtils.isEmpty(description)){
				titleView.setText(R.string.add_alt_text);
				titleView.setTextColor(UiUtils.getThemeColor(titleView.getContext(), R.attr.colorM3OnSurfaceVariant));
			}else{
				titleView.setText(description);
				titleView.setTextColor(UiUtils.getThemeColor(titleView.getContext(), R.attr.colorM3OnSurface));
			}
		}

		public void setUseErrorColors(boolean use){
			if(errorColors==use)
				return;
			errorColors=use;
			if(errorTransitionAnimator!=null)
				errorTransitionAnimator.cancel();
			AnimatorSet set=new AnimatorSet();
			int color1, color2, color3;
			if(use){
				color1=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3ErrorContainer);
				color2=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Error);
				color3=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnErrorContainer);
			}else{
				color1=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Surface);
				color2=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnSurface);
				color3=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnSurfaceVariant);
			}
			set.playTogether(
					ObjectAnimator.ofArgb(view, "backgroundColor", ((ColorDrawable)view.getBackground()).getColor(), color1),
					ObjectAnimator.ofArgb(titleView, "textColor", titleView.getCurrentTextColor(), color2),
					ObjectAnimator.ofArgb(subtitleView, "textColor", subtitleView.getCurrentTextColor(), color3),
					ObjectAnimator.ofArgb(removeButton.getDrawable(), "tint", subtitleView.getCurrentTextColor(), color3)
			);
			editButton.getDrawable().setTint(color3);
			set.setDuration(250);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					errorTransitionAnimator=null;
				}
			});
			set.start();
			errorTransitionAnimator=set;
		}
	}

	enum AttachmentUploadState{
		QUEUED,
		UPLOADING,
		PROCESSING,
		ERROR,
		DONE
	}

	private class AttachmentDragListener implements ReorderableLinearLayout.OnDragListener{
		private final HashMap<View, Animator> currentAnimations=new HashMap<>();

		@Override
		public void onSwapItems(int oldIndex, int newIndex){
			attachments.add(newIndex, attachments.remove(oldIndex));
		}

		@Override
		public void onDragStart(View view){
			if(currentAnimations.containsKey(view))
				currentAnimations.get(view).cancel();
			fragment.mainLayout.setClipChildren(false);
			AnimatorSet set=new AnimatorSet();
			DraftMediaAttachment att=(DraftMediaAttachment) view.getTag();
			att.dragLayer.setVisibility(View.VISIBLE);
			set.playTogether(
					ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, V.dp(3)),
					ObjectAnimator.ofFloat(att.dragLayer, View.ALPHA, 0.16f)
			);
			set.setDuration(150);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					currentAnimations.remove(view);
				}
			});
			currentAnimations.put(view, set);
			set.start();
		}

		@Override
		public void onDragEnd(View view){
			if(currentAnimations.containsKey(view))
				currentAnimations.get(view).cancel();
			AnimatorSet set=new AnimatorSet();
			DraftMediaAttachment att=(DraftMediaAttachment) view.getTag();
			set.playTogether(
					ObjectAnimator.ofFloat(att.dragLayer, View.ALPHA, 0),
					ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, 0),
					ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0),
					ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0)
			);
			set.setDuration(200);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					if(currentAnimations.isEmpty())
						fragment.mainLayout.setClipChildren(true);
					att.dragLayer.setVisibility(View.GONE);
					currentAnimations.remove(view);
				}
			});
			currentAnimations.put(view, set);
			set.start();
		}
	}
}
