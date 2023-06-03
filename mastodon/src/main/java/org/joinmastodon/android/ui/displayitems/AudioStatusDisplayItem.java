package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.AudioPlayerService;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.AudioAttachmentBackgroundDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.palette.graphics.Palette;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class AudioStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	public final Attachment attachment;
	private final ImageLoaderRequest imageRequest;

	public AudioStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status, Attachment attachment){
		super(parentID, parentFragment);
		this.status=status;
		this.attachment=attachment;
		imageRequest=new UrlImageLoaderRequest(TextUtils.isEmpty(attachment.previewUrl) ? status.account.avatarStatic : attachment.previewUrl, V.dp(100), V.dp(100));
	}

	@Override
	public Type getType(){
		return Type.AUDIO;
	}

	@Override
	public int getImageCount(){
		return 1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return imageRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<AudioStatusDisplayItem> implements AudioPlayerService.Callback, ImageLoaderViewHolder{
		private final ImageButton playPauseBtn, forwardBtn, rewindBtn;
		private final TextView time;
		private final ImageView image;
		private final FrameLayout content;
		private final AudioAttachmentBackgroundDrawable bgDrawable;

		private int lastKnownPosition;
		private long lastKnownPositionTime;
		private int lastPosSeconds=-1;
		private AudioPlayerService.PlayState state;

		private final Runnable positionUpdater=this::updatePosition;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_audio, parent);
			playPauseBtn=findViewById(R.id.play_pause_btn);
			time=findViewById(R.id.time);
			image=findViewById(R.id.image);
			content=findViewById(R.id.content);
			forwardBtn=findViewById(R.id.forward_btn);
			rewindBtn=findViewById(R.id.rewind_btn);
			playPauseBtn.setOnClickListener(this::onPlayPauseClick);
			itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
				@Override
				public void onViewAttachedToWindow(View v){
					AudioPlayerService.registerCallback(Holder.this);
				}

				@Override
				public void onViewDetachedFromWindow(View v){
					AudioPlayerService.unregisterCallback(Holder.this);
				}
			});
			forwardBtn.setOnClickListener(this::onSeekButtonClick);
			rewindBtn.setOnClickListener(this::onSeekButtonClick);

			image.setOutlineProvider(OutlineProviders.OVAL);
			image.setClipToOutline(true);
			content.setBackground(bgDrawable=new AudioAttachmentBackgroundDrawable());
		}

		@Override
		public void onBind(AudioStatusDisplayItem item){
			int seconds=(int)item.attachment.getDuration();
			String duration=UiUtils.formatMediaDuration(seconds);
			AudioPlayerService service=AudioPlayerService.getInstance();
			if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
				forwardBtn.setVisibility(View.VISIBLE);
				rewindBtn.setVisibility(View.VISIBLE);
				onPlayStateChanged(item.attachment.id, service.getPlayState(), service.getPosition());
				actuallyUpdatePosition();
			}else{
				state=null;
				time.setText(duration);
				forwardBtn.setVisibility(View.INVISIBLE);
				rewindBtn.setVisibility(View.INVISIBLE);
				setPlayButtonPlaying(false, false);
			}

			int mainColor;
			if(item.attachment.meta!=null && item.attachment.meta.colors!=null){
				try{
					mainColor=Color.parseColor(item.attachment.meta.colors.background);
				}catch(IllegalArgumentException x){
					mainColor=0xff808080;
				}
			}else{
				mainColor=0xff808080;
			}
			updateColors(mainColor);
		}

		private void onPlayPauseClick(View v){
			AudioPlayerService service=AudioPlayerService.getInstance();
			if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
				if(state!=AudioPlayerService.PlayState.PAUSED)
					service.pause(true);
				else
					service.play();
			}else{
				AudioPlayerService.start(v.getContext(), item.status, item.attachment);
				onPlayStateChanged(item.attachment.id, AudioPlayerService.PlayState.BUFFERING, 0);
				forwardBtn.setVisibility(View.VISIBLE);
				rewindBtn.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void onPlayStateChanged(String attachmentID, AudioPlayerService.PlayState state, int position){
			if(attachmentID.equals(item.attachment.id)){
				this.lastKnownPosition=position;
				lastKnownPositionTime=SystemClock.uptimeMillis();
				this.state=state;
				setPlayButtonPlaying(state!=AudioPlayerService.PlayState.PAUSED, true);
				if(state==AudioPlayerService.PlayState.PLAYING){
					itemView.postOnAnimation(positionUpdater);
				}else if(state==AudioPlayerService.PlayState.BUFFERING){
					actuallyUpdatePosition();
				}
			}
		}

		@Override
		public void onPlaybackStopped(String attachmentID){
			if(attachmentID.equals(item.attachment.id)){
				state=null;
				setPlayButtonPlaying(false, true);
				forwardBtn.setVisibility(View.INVISIBLE);
				rewindBtn.setVisibility(View.INVISIBLE);
				time.setText(UiUtils.formatMediaDuration((int)item.attachment.getDuration()));
			}
		}

		private void updatePosition(){
			if(state!=AudioPlayerService.PlayState.PLAYING)
				return;
			actuallyUpdatePosition();
			itemView.postOnAnimation(positionUpdater);
		}

		@SuppressLint("SetTextI18n")
		private void actuallyUpdatePosition(){
			double pos=lastKnownPosition/1000.0;
			if(state==AudioPlayerService.PlayState.PLAYING)
				pos+=(SystemClock.uptimeMillis()-lastKnownPositionTime)/1000.0;
			int posSeconds=(int)pos;
			if(posSeconds!=lastPosSeconds){
				lastPosSeconds=posSeconds;
				time.setText(UiUtils.formatMediaDuration(posSeconds)+"/"+UiUtils.formatMediaDuration((int)item.attachment.getDuration()));
			}
		}

		private void updateColors(int mainColor){
			float[] hsv={0, 0, 0};
			float[] hsv2={0, 0, 0};
			Color.colorToHSV(mainColor, hsv);
			boolean isGray=hsv[1]<0.2f;
			boolean isDarkTheme=UiUtils.isDarkTheme();
			hsv2[0]=hsv[0];
			hsv2[1]=isGray ? hsv[1] : (isDarkTheme ? 0.6f : 0.4f);
			hsv2[2]=isDarkTheme ? 0.3f : 0.75f;
			int bgColor=Color.HSVToColor(hsv2);
			hsv2[1]=isGray ? hsv[1] : (isDarkTheme ? 0.3f : 0.6f);
			hsv2[2]=isDarkTheme ? 0.6f : 0.4f;
			bgDrawable.setColors(bgColor, Color.HSVToColor(128, hsv2));

			hsv2[1]=isGray ? hsv[1] : 0.1f;
			hsv2[2]=1;
			int controlsColor=Color.HSVToColor(hsv2);
			time.setTextColor(controlsColor);
			forwardBtn.setColorFilter(controlsColor);
			rewindBtn.setColorFilter(controlsColor);
		}

		private void setPlayButtonPlaying(boolean playing, boolean animated){
			playPauseBtn.setImageResource(playing ? R.drawable.ic_pause_48px : R.drawable.ic_play_arrow_48px);
			playPauseBtn.setContentDescription(item.parentFragment.getString(playing ? R.string.pause : R.string.play));
			if(playing)
				bgDrawable.startAnimation();
			else
				bgDrawable.stopAnimation(animated);
		}

		private void onSeekButtonClick(View v){
			int seekAmount=v.getId()==R.id.forward_btn ? 10_000 : -5_000;
			AudioPlayerService service=AudioPlayerService.getInstance();
			if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
				int newPos=Math.min(Math.max(0, service.getPosition()+seekAmount), (int)(item.attachment.getDuration()*1000));
				service.seekTo(newPos);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			this.image.setImageDrawable(image);
			if((item.attachment.meta==null || item.attachment.meta.colors==null) && image instanceof BitmapDrawable bd){
				Bitmap bitmap=bd.getBitmap();
				if(Build.VERSION.SDK_INT>=26 && bitmap.getConfig()==Bitmap.Config.HARDWARE)
					bitmap=bitmap.copy(Bitmap.Config.ARGB_8888, false);
				int color=Palette.from(bitmap).maximumColorCount(1).generate().getDominantColor(0xff808080);
				updateColors(color);
			}
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
