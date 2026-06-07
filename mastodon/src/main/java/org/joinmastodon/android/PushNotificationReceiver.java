package org.joinmastodon.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.notifications.GetNotificationByID;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.imageloader.ImageLoaderCallback;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class PushNotificationReceiver extends BroadcastReceiver{
	private static final String TAG="PushNotificationReceive";
	private static final int[] BASE85_DECODE_TABLE={
			0xff, 0x44, 0xff, 0x54, 0x53, 0x52, 0x48, 0xff,
			0x4b, 0x4c, 0x46, 0x41, 0xff, 0x3f, 0x3e, 0x45,
			0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
			0x08, 0x09, 0x40, 0xff, 0x49, 0x42, 0x4a, 0x47,
			0x51, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a,
			0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32,
			0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a,
			0x3b, 0x3c, 0x3d, 0x4d, 0xff, 0x4e, 0x43, 0xff,
			0xff, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
			0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20,
			0x21, 0x22, 0x23, 0x4f, 0xff, 0x50, 0xff, 0xff
	};

	public static final int NOTIFICATION_ID=178;

	@Override
	public void onReceive(Context context, Intent intent){
		if(BuildConfig.DEBUG){
			Log.e(TAG, "received: "+intent);
			Bundle extras=intent.getExtras();
			for(String key : extras.keySet()){
				Log.i(TAG, key+" -> "+extras.get(key));
			}
		}
		if("com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())){
			String serverKey=intent.getStringExtra("k");
			String payload=intent.getStringExtra("p");
			String salt=intent.getStringExtra("s");
			String pushAccountID=intent.getStringExtra("x");
			boolean isRFC="1".equals(intent.getStringExtra("rfc"));
			if(!TextUtils.isEmpty(pushAccountID) && (isRFC || !TextUtils.isEmpty(serverKey)) && !TextUtils.isEmpty(payload) && (isRFC || !TextUtils.isEmpty(salt))){
				MastodonAPIController.runInBackground(()->{
					try{
						List<AccountSession> accounts=AccountSessionManager.getInstance().getLoggedInAccounts();
						AccountSession account=null;
						for(AccountSession acc:accounts){
							if(pushAccountID.equals(acc.pushAccountID)){
								account=acc;
								break;
							}
						}
						if(account==null){
							Log.w(TAG, "onReceive: account for id '"+pushAccountID+"' not found");
							return;
						}
						if(account.getLocalPreferences().getNotificationsPauseEndTime()>System.currentTimeMillis()){
							Log.i(TAG, "onReceive: dropping notification because user has paused notifications for this account");
							return;
						}
						String accountID=account.getID();
						if(isRFC!=AccountSessionManager.get(accountID).pushEncryptionFinalRFC){
							Log.i(TAG, "onReceive: isRFC mismatch between client and server");
							return;
						}
						byte[] decodedServerKey, decodedPayload, decodedSalt;
						if(isRFC){
							byte[] rawPayload=decode85(payload);
							if(rawPayload.length<22){
								Log.i("TAG", "onReceive: payload is too short");
								return;
							}
							DataInputStream in=new DataInputStream(new ByteArrayInputStream(rawPayload));
							decodedSalt=new byte[16];
							in.readFully(decodedSalt);
							int rs=in.readInt();
							int idLen=in.read();
							decodedServerKey=new byte[idLen];
							in.readFully(decodedServerKey);
							decodedPayload=new byte[in.available()];
							in.readFully(decodedPayload);
						}else{
							decodedServerKey=decode85(serverKey);
							decodedPayload=decode85(payload);
							decodedSalt=decode85(salt);
						}
						PushNotification pn=AccountSessionManager.getInstance().getAccount(accountID).getPushSubscriptionManager().decryptNotification(decodedServerKey, decodedPayload, decodedSalt);
						if(pn==null){
							Log.i(TAG, "onReceive: failed to decrypt payload");
							return;
						}
						new GetNotificationByID(pn.notificationId)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(org.joinmastodon.android.model.Notification result){
										MastodonAPIController.runInBackground(()->PushNotificationReceiver.this.notify(context, pn, accountID, result));
									}

									@Override
									public void onError(ErrorResponse error){
										MastodonAPIController.runInBackground(()->PushNotificationReceiver.this.notify(context, pn, accountID, null));
									}
								})
								.exec(accountID);
					}catch(Exception x){
						Log.w(TAG, x);
					}
				});
			}else{
				Log.w(TAG, "onReceive: invalid push notification format");
			}
		}
	}

	private static byte[] decode85(String in){
		ByteArrayOutputStream data=new ByteArrayOutputStream();
		int block=0;
		int n=0;
		for(char c:in.toCharArray()){
			if(c>=32 && c<128 && BASE85_DECODE_TABLE[c-32]!=0xff){
				int value=BASE85_DECODE_TABLE[c-32];
				block=block*85+value;
				n++;
				if(n==5){
					data.write(block >> 24);
					data.write(block >> 16);
					data.write(block >> 8);
					data.write(block);
					block=0;
					n=0;
				}
			}
		}
		if(n>=4)
			data.write(block >> 16);
		if(n>=3)
			data.write(block >> 8);
		if(n>=2)
			data.write(block);
		return data.toByteArray();
	}

	private void notify(Context context, PushNotification pn, String accountID, org.joinmastodon.android.model.Notification notification){
		if(TextUtils.isEmpty(pn.icon)){
			doNotify(context, pn, accountID, notification, null);
		}else{
			ImageCache.getInstance(context).get(new UrlImageLoaderRequest(pn.icon, V.dp(50), V.dp(50)), null, new ImageLoaderCallback(){
				@Override
				public void onImageLoaded(ImageLoaderRequest req, Drawable image){
					doNotify(context, pn, accountID, notification, image);
				}

				@Override
				public void onImageLoadingFailed(ImageLoaderRequest req, Throwable error){
					doNotify(context, pn, accountID, notification, null);
				}
			}, true);
		}
	}

	private void doNotify(Context context, PushNotification pn, String accountID, org.joinmastodon.android.model.Notification notification, Drawable avatar){
		NotificationManager nm=context.getSystemService(NotificationManager.class);
		Account self=AccountSessionManager.getInstance().getAccount(accountID).self;
		String accountName="@"+self.username+"@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
		Notification.Builder builder;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			boolean hasGroup=false;
			int version=AccountSessionManager.get(accountID).getRawLocalPreferences().getInt("notificationChannelsVersion", 1);
			List<NotificationChannelGroup> channelGroups=nm.getNotificationChannelGroups();
			for(NotificationChannelGroup group:channelGroups){
				if(group.getId().equals(accountID)){
					hasGroup=true;
					break;
				}
			}
			if(!hasGroup || version!=2){
				NotificationChannelGroup group=new NotificationChannelGroup(accountID, accountName);
				nm.createNotificationChannelGroup(group);
				List<NotificationChannel> channels=Arrays.stream(PushNotification.Type.values())
						.map(type->{
							NotificationChannel channel=new NotificationChannel(accountID+"_"+type, context.getString(type.localizedName), NotificationManager.IMPORTANCE_DEFAULT);
							channel.setLightColor(context.getColor(R.color.primary_700));
							channel.enableLights(true);
							channel.setGroup(accountID);
							return channel;
						})
						.collect(Collectors.toList());
				nm.createNotificationChannels(channels);
				AccountSessionManager.get(accountID).getRawLocalPreferences().edit().putInt("notificationChannelsVersion", 2).apply();
			}
			builder=new Notification.Builder(context, accountID+"_"+pn.notificationType);
		}else{
			builder=new Notification.Builder(context)
					.setPriority(Notification.PRIORITY_DEFAULT)
					.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
		}
		Intent contentIntent=new Intent(context, MainActivity.class);
		contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		contentIntent.putExtra("fromNotification", true);
		contentIntent.putExtra("accountID", accountID);
		if(notification!=null){
			contentIntent.putExtra("notification", Parcels.wrap(notification));
		}
		builder.setContentTitle(pn.title)
				.setContentText(pn.body)
				.setStyle(new Notification.BigTextStyle().bigText(pn.body))
				.setSmallIcon(R.drawable.ic_ntf_logo)
				.setContentIntent(PendingIntent.getActivity(context, (accountID+pn.notificationId).hashCode() & 0xFFFF, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
				.setWhen(notification==null ? System.currentTimeMillis() : notification.createdAt.toEpochMilli())
				.setShowWhen(true)
				.setCategory(Notification.CATEGORY_SOCIAL)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setLights(context.getColor(R.color.primary_700), 500, 1000)
				.setColor(context.getColor(R.color.primary_700))
				.setGroup(accountID);
		if(avatar!=null){
			builder.setLargeIcon(UiUtils.getBitmapFromDrawable(avatar));
		}
		if(AccountSessionManager.getInstance().getLoggedInAccounts().size()>1){
			builder.setSubText(accountName);
		}
		String notificationTag=accountID+"_"+(notification==null ? 0 : notification.id);
		if(notification!=null && (notification.type==NotificationType.MENTION)){
			ArrayList<String> mentions=new ArrayList<>();
			String ownID=AccountSessionManager.getInstance().getAccount(accountID).self.id;
			if(!notification.status.account.id.equals(ownID))
				mentions.add('@'+notification.status.account.acct);
			for(Mention mention:notification.status.mentions){
				if(mention.id.equals(ownID))
					continue;
				String m='@'+mention.acct;
				if(!mentions.contains(m))
					mentions.add(m);
			}
			String replyPrefix=mentions.isEmpty() ? "" : TextUtils.join(" ", mentions)+" ";

			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
				Intent replyIntent=new Intent(context, NotificationActionHandlerService.class);
				replyIntent.putExtra("action", "reply");
				replyIntent.putExtra("account", accountID);
				replyIntent.putExtra("post", notification.status.id);
				replyIntent.putExtra("notificationTag", notificationTag);
				replyIntent.putExtra("visibility", notification.status.visibility.toString());
				replyIntent.putExtra("replyPrefix", replyPrefix);
				builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_reply_24px),
						context.getString(R.string.button_reply), PendingIntent.getService(context, (accountID+pn.notificationId+"reply").hashCode(), replyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE))
								.addRemoteInput(new RemoteInput.Builder("replyText").setLabel(context.getString(R.string.button_reply)).build())
								.build());
			}

			Intent favIntent=new Intent(context, NotificationActionHandlerService.class);
			favIntent.putExtra("action", "favorite");
			favIntent.putExtra("account", accountID);
			favIntent.putExtra("post", notification.status.id);
			favIntent.putExtra("notificationTag", notificationTag);
			builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_star_24px),
					context.getString(R.string.button_favorite), PendingIntent.getService(context, (accountID+pn.notificationId+"favorite").hashCode(), favIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)).build());

			PendingIntent boostActionIntent;
			if(notification.status.visibility!=StatusPrivacy.DIRECT){
				Intent boostIntent=new Intent(context, NotificationActionHandlerService.class);
				boostIntent.putExtra("action", "boost");
				boostIntent.putExtra("account", accountID);
				boostIntent.putExtra("post", notification.status.id);
				boostIntent.putExtra("notificationTag", notificationTag);
				boostActionIntent=PendingIntent.getService(context, (accountID+pn.notificationId+"boost").hashCode(), boostIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			}else{
				boostActionIntent=null;
			}
			builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_boost_24px),
					context.getString(R.string.button_reblog), boostActionIntent).build());
		}
		nm.notify(notificationTag, NOTIFICATION_ID, builder.build());

		StatusBarNotification[] activeNotifications=nm.getActiveNotifications();
		ArrayList<String> summaryLines=new ArrayList<>();
		int notificationCount=0;
		for(StatusBarNotification sbn:activeNotifications){
			String tag=sbn.getTag();
			if(tag!=null && tag.startsWith(accountID+"_")){
				if((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY)==0){
					if(summaryLines.size()<5){
						summaryLines.add(sbn.getNotification().extras.getString("android.title"));
					}
					notificationCount++;
				}
			}
		}

		if(summaryLines.size()>1){
			Notification.Builder summaryBuilder;
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
				summaryBuilder=new Notification.Builder(context, accountID+"_"+pn.notificationType);
			}else{
				summaryBuilder=new Notification.Builder(context)
						.setPriority(Notification.PRIORITY_DEFAULT);
			}
			Notification.InboxStyle inboxStyle=new Notification.InboxStyle();
			for(String line:summaryLines){
				inboxStyle.addLine(line);
			}
			summaryBuilder.setContentTitle(context.getString(R.string.app_name))
					.setContentText(context.getResources().getQuantityString(R.plurals.x_new_notifications, notificationCount, notificationCount))
					.setSmallIcon(R.drawable.ic_ntf_logo)
					.setColor(context.getColor(R.color.primary_700))
					.setContentIntent(PendingIntent.getActivity(context, accountID.hashCode() & 0xFFFF, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
					.setWhen(notification==null ? System.currentTimeMillis() : notification.createdAt.toEpochMilli())
					.setShowWhen(true)
					.setCategory(Notification.CATEGORY_SOCIAL)
					.setAutoCancel(true)
					.setGroup(accountID)
					.setGroupSummary(true)
					.setStyle(inboxStyle.setSummaryText(accountName));
			nm.notify(accountID+"_summary", NOTIFICATION_ID, summaryBuilder.build());
		}
	}
}
