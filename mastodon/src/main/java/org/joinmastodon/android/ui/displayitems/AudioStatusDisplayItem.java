package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.joinmastodon.android.AudioPlayerService;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.drawables.SeekBarThumbDrawable;

public class AudioStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	public final Attachment attachment;

	public AudioStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status, Attachment attachment){
		super(parentID, parentFragment);
		this.status=status;
		this.attachment=attachment;
	}

	@Override
	public Type getType(){
		return Type.AUDIO;
	}

	public static class Holder extends StatusDisplayItem.Holder<AudioStatusDisplayItem> implements AudioPlayerService.Callback{
		private final ImageButton playPauseBtn;
		private final TextView time;
		private final SeekBar seekBar;

		private int lastKnownPosition;
		private long lastKnownPositionTime;
		private boolean playing;
		private int lastRemainingSeconds=-1;
		private boolean seekbarBeingDragged;

		private Runnable positionUpdater=this::updatePosition;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_audio, parent);
			playPauseBtn=findViewById(R.id.play_pause_btn);
			time=findViewById(R.id.time);
			seekBar=findViewById(R.id.seekbar);
			seekBar.setThumb(new SeekBarThumbDrawable(context));
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
			seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
					if(fromUser){
						int seconds=(int)(seekBar.getProgress()/10000.0*item.attachment.getDuration());
						time.setText(formatDuration(seconds));
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar){
					seekbarBeingDragged=true;
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar){
					AudioPlayerService service=AudioPlayerService.getInstance();
					if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
						service.seekTo((int)(seekBar.getProgress()/10000.0*item.attachment.getDuration()*1000.0));
					}
					seekbarBeingDragged=false;
					if(playing)
						itemView.postOnAnimation(positionUpdater);
				}
			});
		}

		@Override
		public void onBind(AudioStatusDisplayItem item){
			int seconds=(int)item.attachment.getDuration();
			String duration=formatDuration(seconds);
			// Some fonts (not Roboto) have different-width digits. 0 is supposedly the widest.
			time.getLayoutParams().width=(int)Math.ceil(Math.max(time.getPaint().measureText("-"+duration),
					time.getPaint().measureText("-"+duration.replaceAll("\\d", "0"))));
			time.setText(duration);
			AudioPlayerService service=AudioPlayerService.getInstance();
			if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
				seekBar.setEnabled(true);
				onPlayStateChanged(item.attachment.id, service.isPlaying(), service.getPosition());
			}else{
				seekBar.setEnabled(false);
			}
		}

		private void onPlayPauseClick(View v){
			AudioPlayerService service=AudioPlayerService.getInstance();
			if(service!=null && service.getAttachmentID().equals(item.attachment.id)){
				if(playing)
					service.pause(true);
				else
					service.play();
			}else{
				AudioPlayerService.start(v.getContext(), item.status, item.attachment);
				onPlayStateChanged(item.attachment.id, true, 0);
				seekBar.setEnabled(true);
			}
		}

		@Override
		public void onPlayStateChanged(String attachmentID, boolean playing, int position){
			if(attachmentID.equals(item.attachment.id)){
				this.lastKnownPosition=position;
				lastKnownPositionTime=SystemClock.uptimeMillis();
				this.playing=playing;
				playPauseBtn.setImageResource(playing ? R.drawable.ic_fluent_pause_circle_24_filled : R.drawable.ic_fluent_play_circle_24_filled);
				if(!playing){
					lastRemainingSeconds=-1;
					time.setText(formatDuration((int) item.attachment.getDuration()));
				}else{
					itemView.postOnAnimation(positionUpdater);
				}
			}
		}

		@Override
		public void onPlaybackStopped(String attachmentID){
			if(attachmentID.equals(item.attachment.id)){
				playing=false;
				playPauseBtn.setImageResource(R.drawable.ic_fluent_play_circle_24_filled);
				seekBar.setProgress(0);
				seekBar.setEnabled(false);
				time.setText(formatDuration((int)item.attachment.getDuration()));
			}
		}

		private String formatDuration(int seconds){
			if(seconds>=3600)
				return String.format("%d:%02d:%02d", seconds/3600, seconds%3600/60, seconds%60);
			else
				return String.format("%d:%02d", seconds/60, seconds%60);
		}

		private void updatePosition(){
			if(!playing || seekbarBeingDragged)
				return;
			double pos=lastKnownPosition/1000.0+(SystemClock.uptimeMillis()-lastKnownPositionTime)/1000.0;
			seekBar.setProgress((int)Math.round(pos/item.attachment.getDuration()*10000.0));
			itemView.postOnAnimation(positionUpdater);
			int remainingSeconds=(int)(item.attachment.getDuration()-pos);
			if(remainingSeconds!=lastRemainingSeconds){
				lastRemainingSeconds=remainingSeconds;
				time.setText("-"+formatDuration(remainingSeconds));
			}
		}
	}
}
