package org.joinmastodon.android.api;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.requests.notifications.RegisterForPushNotifications;
import org.joinmastodon.android.api.requests.notifications.UpdatePushSettings;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.PushSubscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class PushSubscriptionManager{
	private static final String EC_CURVE_NAME="prime256v1";
	private static final byte[] P256_HEAD=new byte[]{(byte)0x30,(byte)0x59,(byte)0x30,(byte)0x13,(byte)0x06,(byte)0x07,(byte)0x2a,
			(byte)0x86,(byte)0x48,(byte)0xce,(byte)0x3d,(byte)0x02,(byte)0x01,(byte)0x06,(byte)0x08,(byte)0x2a,(byte)0x86,
			(byte)0x48,(byte)0xce,(byte)0x3d,(byte)0x03,(byte)0x01,(byte)0x07,(byte)0x03,(byte)0x42,(byte)0x00};

	private static final String TAG="PushSubscriptionManager";
	private static final long TOKEN_REFRESH_INTERVAL=30*24*60*60*1000L;

	private String accountID;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private byte[] authKey;

	public PushSubscriptionManager(String accountID){
		this.accountID=accountID;
	}

	public static void resetLocalPreferences(){
		boolean forceNonRFC=isForceNonRFC();
		getPrefs().edit().clear().apply();
		setForceNonRFC(forceNonRFC);
		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			session.pushToken=null;
			session.pushTokenVersion=0;
			AccountSessionManager.getInstance().writeAccountPushSettings(session.getID());
		}
	}

	public static void tryRegisterFCM(){
		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			session.getPushSubscriptionManager().registerFCM();
		}
	}

	public void registerFCM(){
		AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
		int tokenVersion=Math.max(getPrefs().getInt("version", 0), session.pushTokenVersion);
		long tokenLastRefreshed=session.pushTokenLastRefresh;
		if(!TextUtils.isEmpty(session.pushToken) && tokenVersion==BuildConfig.VERSION_CODE && System.currentTimeMillis()-tokenLastRefreshed<TOKEN_REFRESH_INTERVAL){
			registerAccountForPush(session.pushSubscription);
			return;
		}
		if(tokenVersion<145){ // Quote notifications added
			if(session.pushSubscription!=null){
				session.pushSubscription.alerts.quote=session.pushSubscription.alerts.mention;
				AccountSessionManager.getInstance().writeAccountPushSettings(session.getID());
			}
		}
		Log.i(TAG, "["+accountID+"] registerFCM: no token found, token due for refresh, or app was updated. Trying to get push token...");
		if(session.pushAccountID==null || tokenVersion<184){
			session.pushAccountID=UUID.randomUUID().toString();
			AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
		}

		Intent intent=new Intent("com.google.android.c2dm.intent.REGISTER");
		intent.setPackage("com.google.android.gms");
		intent.putExtra("app", PendingIntent.getBroadcast(MastodonApp.context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE));
		String sender=session.getInstanceInfo().getVapidPublicKey();
		if(sender==null){
			Log.w(TAG, "Can't register account "+accountID+" for push because the server does not provide a public key");
			return;
		}
		String subtype="wp:https://"+session.domain+"/#"+session.pushAccountID;
		intent.putExtra("sender", sender);
		intent.putExtra("subscription", sender);
		intent.putExtra("X-subscription", sender);
		intent.putExtra("subtype", subtype);
		intent.putExtra("X-subtype", subtype);
		intent.putExtra("scope", "GCM");
		intent.putExtra("kid", "|ID|"+accountID+"|");
		MastodonApp.context.startService(intent);
	}

	public static void setForceNonRFC(boolean force){
		getPrefs().edit().putBoolean("forceNonRFC", force).apply();
	}

	public static boolean isForceNonRFC(){
		return getPrefs().getBoolean("forceNonRFC", false);
	}

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("push", Context.MODE_PRIVATE);
	}

	public void registerAccountForPush(PushSubscription subscription){
		AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
		if(session==null)
			return;
		if(TextUtils.isEmpty(session.pushToken))
			throw new IllegalStateException("No device push token available");
		MastodonAPIController.runInBackground(()->{
			Log.d(TAG, "registerAccountForPush: started for "+accountID);
			String encodedPublicKey, encodedAuthKey, pushAccountID=session.pushAccountID;
			if(session.hasPushCredentials()){
				if(!loadKeys(session))
					return;
				encodedAuthKey=session.pushAuthKey;
			}else{
				try{
					KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");
					ECGenParameterSpec spec=new ECGenParameterSpec(EC_CURVE_NAME);
					generator.initialize(spec);
					KeyPair keyPair=generator.generateKeyPair();
					publicKey=keyPair.getPublic();
					privateKey=keyPair.getPrivate();
					authKey=new byte[16];
					SecureRandom secureRandom=new SecureRandom();
					secureRandom.nextBytes(authKey);
					byte[] randomAccountID=new byte[16];
					secureRandom.nextBytes(randomAccountID);
					session.pushPrivateKey=Base64.encodeToString(privateKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
					session.pushPublicKey=Base64.encodeToString(publicKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
					session.pushAuthKey=encodedAuthKey=Base64.encodeToString(authKey, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
					AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
				}catch(NoSuchAlgorithmException|InvalidAlgorithmParameterException e){
					Log.e(TAG, "registerAccountForPush: error generating encryption key", e);
					return;
				}
			}
			encodedPublicKey=Base64.encodeToString(serializeRawPublicKey(publicKey), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
			session.needReRegisterForPush=true;
			AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
			boolean isRFC=(!BuildConfig.DEBUG || !isForceNonRFC()) && session.getInstanceInfo().getApiVersion()>=4;
			new RegisterForPushNotifications("https://fcm.googleapis.com/fcm/send/"+session.pushToken,
					encodedPublicKey,
					encodedAuthKey,
					subscription==null ? PushSubscription.Alerts.ofAll() : subscription.alerts,
					subscription==null ? PushSubscription.Policy.ALL : subscription.policy,
					pushAccountID, isRFC)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(PushSubscription result){
							MastodonAPIController.runInBackground(()->{
								AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
								if(session==null)
									return;
								session.pushSubscription=result;
								session.needReRegisterForPush=false;
								session.pushEncryptionFinalRFC=isRFC;
								AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
								Log.d(TAG, "Successfully registered "+accountID+" for push notifications");
							});
						}

						@Override
						public void onError(ErrorResponse error){
						}
					})
					.exec(accountID);
		});
	}

	public void updatePushSettings(PushSubscription subscription){
		new UpdatePushSettings(subscription.alerts, subscription.policy)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(PushSubscription result){
						AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
						if(session==null)
							return;
						if(result.policy!=subscription.policy)
							result.policy=subscription.policy;
						session.pushSubscription=result;
						session.needUpdatePushSettings=false;
						AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
					}

					@Override
					public void onError(ErrorResponse error){
						if(((MastodonErrorResponse)error).httpStatus==404){ // Not registered for push, register now
							registerAccountForPush(subscription);
						}else{
							AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
							if(session==null)
								return;
							session.needUpdatePushSettings=true;
							session.pushSubscription=subscription;
							AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
						}
					}
				})
				.exec(accountID);
	}

	private boolean loadKeys(AccountSession session){
		try{
			KeyFactory kf=KeyFactory.getInstance("EC");
			privateKey=kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(session.pushPrivateKey, Base64.URL_SAFE)));
			publicKey=kf.generatePublic(new X509EncodedKeySpec(Base64.decode(session.pushPublicKey, Base64.URL_SAFE)));
			authKey=Base64.decode(session.pushAuthKey, Base64.URL_SAFE);
			return true;
		}catch(NoSuchAlgorithmException|InvalidKeySpecException x){
			Log.e(TAG, "Error loading private key", x);
			return false;
		}
	}

	static PublicKey deserializeRawPublicKey(byte[] rawBytes){
		if(rawBytes.length!=65 && rawBytes.length!=64)
			return null;
		try{
			KeyFactory kf=KeyFactory.getInstance("EC");
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			os.write(P256_HEAD);
			if(rawBytes.length==64)
				os.write(4);
			os.write(rawBytes);
			return kf.generatePublic(new X509EncodedKeySpec(os.toByteArray()));
		}catch(NoSuchAlgorithmException|InvalidKeySpecException|IOException x){
			Log.e(TAG, "deserializeRawPublicKey", x);
		}
		return null;
	}

	private static byte[] serializeRawPublicKey(PublicKey key){
		ECPoint point=((ECPublicKey)key).getW();
		byte[] x=point.getAffineX().toByteArray();
		byte[] y=point.getAffineY().toByteArray();
		if(x.length>32)
			x=Arrays.copyOfRange(x, x.length-32, x.length);
		if(y.length>32)
			y=Arrays.copyOfRange(y, y.length-32, y.length);
		byte[] result=new byte[65];
		result[0]=4;
		System.arraycopy(x, 0, result, 1+(32-x.length), x.length);
		System.arraycopy(y, 0, result, result.length-y.length, y.length);
		return result;
	}

	public PushNotification decryptNotification(byte[] serverKeyBytes, byte[] payload, byte[] salt){
		if(privateKey==null){
			if(!loadKeys(AccountSessionManager.getInstance().getAccount(accountID)))
				return null;
		}
		String decryptedStr=decryptNotification(serverKeyBytes, payload, salt, authKey, publicKey, privateKey, AccountSessionManager.get(accountID).pushEncryptionFinalRFC);
		if(decryptedStr==null)
			return null;
		PushNotification notification=MastodonAPIController.gson.fromJson(decryptedStr, PushNotification.class);
		try{
			notification.postprocess();
		}catch(IOException x){
			Log.e(TAG, "decryptNotification: error verifying notification object", x);
			return null;
		}
		return notification;
	}

	static String decryptNotification(byte[] serverKeyBytes, byte[] payload, byte[] salt, byte[] authKey, PublicKey publicKey, PrivateKey privateKey, boolean useFinalRFC){
		PublicKey serverKey=deserializeRawPublicKey(serverKeyBytes);
		byte[] ecdhSecret;
		try{
			KeyAgreement keyAgreement=KeyAgreement.getInstance("ECDH");
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(serverKey, true);
			ecdhSecret=keyAgreement.generateSecret();
		}catch(NoSuchAlgorithmException|InvalidKeyException x){
			Log.e(TAG, "decryptNotification: error doing key exchange", x);
			return null;
		}
		byte[] key, nonce;
		try{
			if(useFinalRFC){
				byte[] ikm=hkdf(authKey, ecdhSecret, info("WebPush: info", publicKey, serverKey, false), 32);
				key=hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.UTF_8), 16);
				nonce=hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.UTF_8), 12);
			}else{
				byte[] secondSalt=hkdf(authKey, ecdhSecret, "Content-Encoding: auth\0".getBytes(StandardCharsets.UTF_8), 32);
				byte[] keyInfo=info("Content-Encoding: aesgcm", publicKey, serverKey, true);
				key=hkdf(salt, secondSalt, keyInfo, 16);
				byte[] nonceInfo=info("Content-Encoding: nonce", publicKey, serverKey, true);
				nonce=hkdf(salt, secondSalt, nonceInfo, 12);
			}
		}catch(NoSuchAlgorithmException|InvalidKeyException x){
			Log.e(TAG, "decryptNotification: error deriving key", x);
			return null;
		}
		String decryptedStr;
		try{
			Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
			SecretKeySpec aesKey=new SecretKeySpec(key, "AES");
			GCMParameterSpec iv=new GCMParameterSpec(128, nonce);
			cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
			byte[] decrypted=cipher.doFinal(payload);
			if(useFinalRFC){
				if(decrypted[decrypted.length-1]!=2){
					Log.i(TAG, "decryptNotification: invalid padding byte");
					return null;
				}
				decryptedStr=new String(decrypted, 0, decrypted.length-1, StandardCharsets.UTF_8);
			}else{
				decryptedStr=new String(decrypted, 2, decrypted.length-2, StandardCharsets.UTF_8);
			}
			if(BuildConfig.DEBUG)
				Log.i(TAG, "decryptNotification: notification json "+decryptedStr);
			return decryptedStr;
		}catch(NoSuchAlgorithmException|NoSuchPaddingException|InvalidAlgorithmParameterException|InvalidKeyException|BadPaddingException|IllegalBlockSizeException x){
			Log.e(TAG, "decryptNotification: error decrypting payload", x);
			return null;
		}
	}

	private static byte[] hkdf(byte[] firstSalt, byte[] secondSalt, byte[] info, int length) throws NoSuchAlgorithmException, InvalidKeyException{
		Mac hmacContext=Mac.getInstance("HmacSHA256");
		hmacContext.init(new SecretKeySpec(firstSalt, "HmacSHA256"));
		byte[] hmac=hmacContext.doFinal(secondSalt);
		hmacContext.init(new SecretKeySpec(hmac, "HmacSHA256"));
		hmacContext.update(info);
		byte[] result=hmacContext.doFinal(new byte[]{1});
		return result.length<=length ? result : Arrays.copyOfRange(result, 0, length);
	}

	private static byte[] info(String header, PublicKey clientPublicKey, PublicKey serverPublicKey, boolean includeLength){
		ByteArrayOutputStream info=new ByteArrayOutputStream();
		try{
			info.write(header.getBytes(StandardCharsets.UTF_8));
			info.write(0);
			if(includeLength){
				info.write("P-256".getBytes(StandardCharsets.UTF_8));
				info.write(0);
				info.write(0);
				info.write(65);
			}
			info.write(serializeRawPublicKey(clientPublicKey));
			if(includeLength){
				info.write(0);
				info.write(65);
			}
			info.write(serializeRawPublicKey(serverPublicKey));
		}catch(IOException ignore){}
		return info.toByteArray();
	}

	public static class RegistrationReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent){
			if("com.google.android.c2dm.intent.REGISTRATION".equals(intent.getAction())){
				if(intent.hasExtra("registration_id")){
					if(BuildConfig.DEBUG){
						Bundle extras=intent.getExtras();
						for(String key:extras.keySet()){
							Log.i(TAG, key+" -> "+extras.get(key));
						}
					}
					String token=intent.getStringExtra("registration_id");
					if(token==null || !token.startsWith("|ID|")){
						Log.w(TAG, "FCM token does not start with |ID|");
						return;
					}
					String[] parts=token.substring(4).split("\\|");
					String accountID=parts[0];
					token=parts[1].substring(1);
					AccountSession session;
					try{
						session=AccountSessionManager.get(accountID);
					}catch(IllegalStateException x){
						Log.w(TAG, x);
						return;
					}
					session.pushToken=token;
					session.pushTokenVersion=BuildConfig.VERSION_CODE;
					session.pushTokenLastRefresh=System.currentTimeMillis();
					AccountSessionManager.getInstance().writeAccountPushSettings(accountID);
					Log.i(TAG, "Successfully registered for FCM");
					session.getPushSubscriptionManager().registerAccountForPush(session.pushSubscription);
				}else{
					Log.e(TAG, "FCM registration intent did not contain registration_id: "+intent);
					Bundle extras=intent.getExtras();
					for(String key:extras.keySet()){
						Log.i(TAG, key+" -> "+extras.get(key));
					}
				}
			}
		}
	}
}
