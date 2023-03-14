package org.joinmastodon.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.parceler.Parcels;

import java.io.IOException;
import java.util.HashSet;

import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class AudioPlayerService extends Service{
	private static final int NOTIFICATION_SERVICE=1;
	private static final String TAG="AudioPlayerService";
	private static final String ACTION_PLAY_PAUSE="org.joinmastodon.android.AUDIO_PLAY_PAUSE";
	private static final String ACTION_STOP="org.joinmastodon.android.AUDIO_STOP";

	private static AudioPlayerService instance;

	private Status status;
	private Attachment attachment;
	private NotificationManager nm;
	private MediaSession session;
	private MediaPlayer player;
	private boolean playerReady;
	private Bitmap statusAvatar;
	private static HashSet<Callback> callbacks=new HashSet<>();
	private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener=this::onAudioFocusChanged;
	private boolean resumeAfterAudioFocusGain;
	private boolean isBuffering=true;

	private BroadcastReceiver receiver=new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
				pause(false);
			}else if(ACTION_PLAY_PAUSE.equals(intent.getAction())){
				if(!playerReady)
					return;
				if(player.isPlaying())
					pause(false);
				else
					play();
			}else if(ACTION_STOP.equals(intent.getAction())){
				stopSelf();
			}
		}
	};

	@Nullable
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public void onCreate(){
		super.onCreate();
		nm=getSystemService(NotificationManager.class);
//		registerReceiver(receiver, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));
		registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		registerReceiver(receiver, new IntentFilter(ACTION_PLAY_PAUSE));
		registerReceiver(receiver, new IntentFilter(ACTION_STOP));
		instance=this;
	}

	@Override
	public void onDestroy(){
		instance=null;
		unregisterReceiver(receiver);
		if(player!=null){
			player.release();
		}
		nm.cancel(NOTIFICATION_SERVICE);
		for(Callback cb:callbacks)
			cb.onPlaybackStopped(attachment.id);
		getSystemService(AudioManager.class).abandonAudioFocus(audioFocusChangeListener);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(player!=null){
			player.release();
			player=null;
			playerReady=false;
		}
		if(attachment!=null){
			for(Callback cb:callbacks)
				cb.onPlaybackStopped(attachment.id);
		}

		status=Parcels.unwrap(intent.getParcelableExtra("status"));
		attachment=Parcels.unwrap(intent.getParcelableExtra("attachment"));

		session=new MediaSession(this, "audioPlayer");
		session.setPlaybackState(new PlaybackState.Builder()
				.setState(PlaybackState.STATE_BUFFERING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
				.setActions(PlaybackState.ACTION_STOP)
				.build());
		MediaMetadata metadata=new MediaMetadata.Builder()
				.putLong(MediaMetadata.METADATA_KEY_DURATION, (long)(attachment.getDuration()*1000))
				.build();
		session.setMetadata(metadata);
		session.setActive(true);
		session.setCallback(new MediaSession.Callback(){
			@Override
			public void onPlay(){
				play();
			}

			@Override
			public void onPause(){
				pause(false);
			}

			@Override
			public void onStop(){
				stopSelf();
			}

			@Override
			public void onSeekTo(long pos){
				seekTo((int)pos);
			}
		});

		Drawable d=ImageCache.getInstance(this).getFromTop(new UrlImageLoaderRequest(status.account.avatar, V.dp(50), V.dp(50)));
		if(d instanceof BitmapDrawable){
			statusAvatar=((BitmapDrawable) d).getBitmap();
		}else if(d!=null){
			statusAvatar=Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			d.draw(new Canvas(statusAvatar));
		}

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			NotificationChannel chan=new NotificationChannel("audioPlayer", getString(R.string.notification_channel_audio_player), NotificationManager.IMPORTANCE_LOW);
			nm.createNotificationChannel(chan);
		}

		updateNotification(false, false);
		getSystemService(AudioManager.class).requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		player=new MediaPlayer();
		player.setOnPreparedListener(this::onPlayerPrepared);
		player.setOnErrorListener(this::onPlayerError);
		player.setOnCompletionListener(this::onPlayerCompletion);
		player.setOnSeekCompleteListener(this::onPlayerSeekCompleted);
		player.setOnInfoListener(this::onPlayerInfo);
		try{
			player.setDataSource(this, Uri.parse(attachment.url));
			player.prepareAsync();
		}catch(IOException x){
			Log.w(TAG, "onStartCommand: error starting media player", x);
		}

		return START_NOT_STICKY;
	}

	private void onPlayerPrepared(MediaPlayer mp){
		Log.i(TAG, "onPlayerPrepared");
		playerReady=true;
		isBuffering=false;
		player.start();
		updateSessionState(false);
	}

	private boolean onPlayerError(MediaPlayer mp, int error, int extra){
		Log.e(TAG, "onPlayerError() called with: mp = ["+mp+"], error = ["+error+"], extra = ["+extra+"]");
		return false;
	}

	private void onPlayerSeekCompleted(MediaPlayer mp){
		updateSessionState(false);
	}

	private void onPlayerCompletion(MediaPlayer mp){
		stopSelf();
	}

	private boolean onPlayerInfo(MediaPlayer mp, int what, int extra){
		switch(what){
			case MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
				isBuffering=true;
				updateSessionState(false);
			}
			case MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
				isBuffering=false;
				updateSessionState(false);
			}
			default -> Log.i(TAG, "onPlayerInfo() called with: mp = ["+mp+"], what = ["+what+"], extra = ["+extra+"]");
		}
		return true;
	}

	private void onAudioFocusChanged(int change){
		switch(change){
			case AudioManager.AUDIOFOCUS_LOSS -> {
				resumeAfterAudioFocusGain=false;
				pause(false);
			}
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				resumeAfterAudioFocusGain=true;
				pause(false);
			}
			case AudioManager.AUDIOFOCUS_GAIN -> {
				if(resumeAfterAudioFocusGain){
					play();
				}else if(isPlaying()){
					player.setVolume(1f, 1f);
				}
			}
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
				if(isPlaying()){
					player.setVolume(.3f, .3f);
				}
			}
		}
	}

	private void updateSessionState(boolean removeNotification){
		session.setPlaybackState(new PlaybackState.Builder()
				.setState(switch(getPlayState()){
					case PLAYING -> PlaybackState.STATE_PLAYING;
					case PAUSED -> PlaybackState.STATE_PAUSED;
					case BUFFERING -> PlaybackState.STATE_BUFFERING;
				}, player.getCurrentPosition(), 1f)
				.setActions(PlaybackState.ACTION_STOP | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SEEK_TO)
				.build());
		updateNotification(!player.isPlaying(), removeNotification);
		for(Callback cb:callbacks)
			cb.onPlayStateChanged(attachment.id, getPlayState(), player.getCurrentPosition());
	}

	private void updateNotification(boolean dismissable, boolean removeNotification){
		Notification.Builder bldr=new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_ntf_logo)
				.setContentTitle(status.account.displayName)
				.setContentText(HtmlParser.strip(status.content))
				.setOngoing(!dismissable)
				.setShowWhen(false)
				.setDeleteIntent(PendingIntent.getBroadcast(this, 3, new Intent(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE));
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			bldr.setChannelId("audioPlayer");
		}
		if(statusAvatar!=null)
			bldr.setLargeIcon(statusAvatar);

		Notification.MediaStyle style=new Notification.MediaStyle().setMediaSession(session.getSessionToken());

		if(playerReady){
			boolean isPlaying=player.isPlaying();
			bldr.addAction(new Notification.Action.Builder(Icon.createWithResource(this, isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_24),
						getString(isPlaying ? R.string.pause : R.string.play),
						PendingIntent.getBroadcast(this, 2, new Intent(ACTION_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE))
					.build());
			style.setShowActionsInCompactView(0);
		}
		bldr.setStyle(style);

		if(dismissable){
			stopForeground(removeNotification);
			if(!removeNotification)
				nm.notify(NOTIFICATION_SERVICE, bldr.build());
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
			startForeground(NOTIFICATION_SERVICE, bldr.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
		}else{
			startForeground(NOTIFICATION_SERVICE, bldr.build());
		}
	}

	public void pause(boolean removeNotification){
		if(player.isPlaying()){
			player.pause();
			updateSessionState(removeNotification);
		}
	}

	public void play(){
		if(playerReady && !player.isPlaying()){
			player.start();
			updateSessionState(false);
		}
	}

	public void seekTo(int offset){
		if(playerReady){
			player.seekTo(offset);
			updateSessionState(false);
		}
	}

	public boolean isPlaying(){
		return playerReady && player.isPlaying();
	}

	public int getPosition(){
		return playerReady ? player.getCurrentPosition() : 0;
	}

	public String getAttachmentID(){
		return attachment.id;
	}

	public PlayState getPlayState(){
		if(isBuffering)
			return PlayState.BUFFERING;
		return player.isPlaying() ? PlayState.PLAYING : PlayState.PAUSED;
	}

	public static void registerCallback(Callback cb){
		callbacks.add(cb);
	}

	public static void unregisterCallback(Callback cb){
		callbacks.remove(cb);
	}

	public static void start(Context context, Status status, Attachment attachment){
		Intent intent=new Intent(context, AudioPlayerService.class);
		intent.putExtra("status", Parcels.wrap(status));
		intent.putExtra("attachment", Parcels.wrap(attachment));
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	public static AudioPlayerService getInstance(){
		return instance;
	}

	public interface Callback{
		void onPlayStateChanged(String attachmentID, PlayState state, int position);
		void onPlaybackStopped(String attachmentID);
	}

	public enum PlayState{
		PLAYING,
		PAUSED,
		BUFFERING
	}
}
