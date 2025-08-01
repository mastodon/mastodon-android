package org.joinmastodon.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.SetStatusFavorited;
import org.joinmastodon.android.api.requests.statuses.SetStatusReblogged;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.util.UUID;

import androidx.annotation.Nullable;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class NotificationActionHandlerService extends Service{
	private static final String TAG="NotificationActionHandl";
	private int runningRequestCount=0;

	@Nullable
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String action=intent.getStringExtra("action");
		String account=intent.getStringExtra("account");
		String postID=intent.getStringExtra("post");
		String notificationTag=intent.getStringExtra("notificationTag");
		if(action==null || account==null || postID==null || notificationTag==null){
			maybeStopSelf();
			return START_NOT_STICKY;
		}
		NotificationManager nm=getSystemService(NotificationManager.class);
		StatusBarNotification notification=findNotification(notificationTag);
		if("reply".equals(action)){
			Bundle remoteInputResults=RemoteInput.getResultsFromIntent(intent);
			if(remoteInputResults==null){
				maybeStopSelf();
				return START_NOT_STICKY;
			}
			CharSequence replyText=remoteInputResults.getCharSequence("replyText");
			if(replyText==null){
				maybeStopSelf();
				return START_NOT_STICKY;
			}
			CreateStatus.Request req=new CreateStatus.Request();
			req.inReplyToId=postID;
			req.status=intent.getStringExtra("replyPrefix")+replyText;
			req.visibility=StatusPrivacy.valueOf(intent.getStringExtra("visibility"));
			runningRequestCount++;
			new CreateStatus(req, UUID.randomUUID().toString())
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Status result){
							E.post(new StatusCreatedEvent(result, account));
							if(notification!=null){
								Notification n=notification.getNotification();
								nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
							}
							runningRequestCount--;
							maybeStopSelf();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(NotificationActionHandlerService.this);
							if(notification!=null){
								Notification n=notification.getNotification();
								nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
							}
							runningRequestCount--;
							maybeStopSelf();
						}
					})
					.exec(account);
		}else if("favorite".equals(action)){
			PendingIntent prevActionIntent;
			if(notification!=null){
				Notification n=notification.getNotification();
				prevActionIntent=n.actions[1].actionIntent;
				n.actions[1].actionIntent=null;
				n.actions[1].title=getString(R.string.button_favorited);
				nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
			}else{
				prevActionIntent=null;
			}
			runningRequestCount++;
			new SetStatusFavorited(postID, true)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Status result){
							E.post(new StatusCountersUpdatedEvent(result, StatusCountersUpdatedEvent.CounterType.FAVORITES));
							runningRequestCount--;
							maybeStopSelf();
						}

						@Override
						public void onError(ErrorResponse error){
							if(notification!=null){
								Notification n=notification.getNotification();
								n.actions[1].actionIntent=prevActionIntent;
								n.actions[1].title=getString(R.string.button_favorite);
								nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
							}
							error.showToast(NotificationActionHandlerService.this);
							runningRequestCount--;
							maybeStopSelf();
						}
					})
					.exec(account);
		}else if("boost".equals(action)){
			PendingIntent prevActionIntent;
			if(notification!=null){
				Notification n=notification.getNotification();
				prevActionIntent=n.actions[2].actionIntent;
				n.actions[2].actionIntent=null;
				n.actions[2].title=getString(R.string.button_reblogged);
				nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
			}else{
				prevActionIntent=null;
			}
			runningRequestCount++;
			new SetStatusReblogged(postID, true)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Status result){
							E.post(new StatusCountersUpdatedEvent(result, StatusCountersUpdatedEvent.CounterType.REBLOGS));
							runningRequestCount--;
							maybeStopSelf();
						}

						@Override
						public void onError(ErrorResponse error){
							if(notification!=null){
								Notification n=notification.getNotification();
								n.actions[2].actionIntent=prevActionIntent;
								n.actions[2].title=getString(R.string.button_reblog);
								nm.notify(notificationTag, PushNotificationReceiver.NOTIFICATION_ID, n);
							}
							error.showToast(NotificationActionHandlerService.this);
							runningRequestCount--;
							maybeStopSelf();
						}
					})
					.exec(account);
		}
		return START_NOT_STICKY;
	}

	private void maybeStopSelf(){
		if(runningRequestCount==0)
			stopSelf();
	}

	private StatusBarNotification findNotification(String tag){
		for(StatusBarNotification sbn:getSystemService(NotificationManager.class).getActiveNotifications()){
			if(tag.equals(sbn.getTag())){
				return sbn;
			}
		}
		return null;
	}
}
