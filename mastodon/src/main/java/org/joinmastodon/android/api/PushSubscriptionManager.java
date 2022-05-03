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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class PushSubscriptionManager{
	private static final String FCM_SENDER_ID="449535203550";
	private static final String EC_CURVE_NAME="prime256v1";
	private static final byte[] P256_HEAD=new byte[]{(byte)0x30,(byte)0x59,(byte)0x30,(byte)0x13,(byte)0x06,(byte)0x07,(byte)0x2a,
			(byte)0x86,(byte)0x48,(byte)0xce,(byte)0x3d,(byte)0x02,(byte)0x01,(byte)0x06,(byte)0x08,(byte)0x2a,(byte)0x86,
			(byte)0x48,(byte)0xce,(byte)0x3d,(byte)0x03,(byte)0x01,(byte)0x07,(byte)0x03,(byte)0x42,(byte)0x00};
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

	private static final String TAG="PushSubscriptionManager";
	public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
	public static final String GSF_PACKAGE = "com.google.android.gms";
	/** Internal parameter used to indicate a 'subtype'. Will not be stored in DB for Nacho. */
	private static final String EXTRA_SUBTYPE = "subtype";
	/** Extra used to indicate which senders (Google API project IDs) can send messages to the app */
	private static final String EXTRA_SENDER = "sender";
	private static final String EXTRA_SCOPE = "scope";
	private static final String KID_VALUE="|ID|1|"; // request ID?

	private static String deviceToken;
	private String accountID;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PublicKey serverKey;
	private byte[] authKey;

	public PushSubscriptionManager(String accountID){
		this.accountID=accountID;
	}

	public static void tryRegisterFCM(){
		deviceToken=getPrefs().getString("deviceToken", null);
		int tokenVersion=getPrefs().getInt("version", 0);
		if(!TextUtils.isEmpty(deviceToken) && tokenVersion==BuildConfig.VERSION_CODE){
			registerAllAccountsForPush(false);
			return;
		}
		Log.i(TAG, "tryRegisterFCM: no token found or app was updated. Trying to get push token...");
		Intent intent = new Intent("com.google.iid.TOKEN_REQUEST");
		intent.setPackage(GSF_PACKAGE);
		intent.putExtra(EXTRA_APPLICATION_PENDING_INTENT,
				PendingIntent.getBroadcast(MastodonApp.context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE));
		intent.putExtra(EXTRA_SENDER, FCM_SENDER_ID);
		intent.putExtra(EXTRA_SUBTYPE, FCM_SENDER_ID);
		intent.putExtra(EXTRA_SCOPE, "*");
		intent.putExtra("kid", KID_VALUE);
		MastodonApp.context.sendBroadcast(intent);
	}

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("push", Context.MODE_PRIVATE);
	}

	public static boolean arePushNotificationsAvailable(){
		return !TextUtils.isEmpty(deviceToken);
	}

	public void registerAccountForPush(PushSubscription subscription){
		if(TextUtils.isEmpty(deviceToken))
			throw new IllegalStateException("No device push token available");
		MastodonAPIController.runInBackground(()->{
			Log.d(TAG, "registerAccountForPush: started for "+accountID);
			String encodedPublicKey, encodedAuthKey, pushAccountID;
			try{
				KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");
				ECGenParameterSpec spec=new ECGenParameterSpec(EC_CURVE_NAME);
				generator.initialize(spec);
				KeyPair keyPair=generator.generateKeyPair();
				publicKey=keyPair.getPublic();
				privateKey=keyPair.getPrivate();
				encodedPublicKey=Base64.encodeToString(serializeRawPublicKey(publicKey), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
				authKey=new byte[16];
				SecureRandom secureRandom=new SecureRandom();
				secureRandom.nextBytes(authKey);
				byte[] randomAccountID=new byte[16];
				secureRandom.nextBytes(randomAccountID);
				AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
				if(session==null)
					return;
				session.pushPrivateKey=Base64.encodeToString(privateKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
				session.pushPublicKey=Base64.encodeToString(publicKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
				session.pushAuthKey=encodedAuthKey=Base64.encodeToString(authKey, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
				session.pushAccountID=pushAccountID=Base64.encodeToString(randomAccountID, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
				AccountSessionManager.getInstance().writeAccountsFile();
			}catch(NoSuchAlgorithmException|InvalidAlgorithmParameterException e){
				Log.e(TAG, "registerAccountForPush: error generating encryption key", e);
				return;
			}
			new RegisterForPushNotifications(deviceToken,
					encodedPublicKey,
					encodedAuthKey,
					subscription==null ? PushSubscription.Alerts.ofAll() : subscription.alerts,
					subscription==null ? PushSubscription.Policy.ALL : subscription.policy,
					pushAccountID)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(PushSubscription result){
							MastodonAPIController.runInBackground(()->{
								serverKey=deserializeRawPublicKey(Base64.decode(result.serverKey, Base64.URL_SAFE));

								AccountSession session=AccountSessionManager.getInstance().tryGetAccount(accountID);
								if(session==null)
									return;
								session.pushSubscription=result;
								AccountSessionManager.getInstance().writeAccountsFile();
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
						AccountSessionManager.getInstance().writeAccountsFile();
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
							AccountSessionManager.getInstance().writeAccountsFile();
						}
					}
				})
				.exec(accountID);
	}

	private PublicKey deserializeRawPublicKey(byte[] rawBytes){
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

	private byte[] serializeRawPublicKey(PublicKey key){
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

	public PushNotification decryptNotification(String k, String p, String s){
		byte[] serverKeyBytes=decode85(k);
		byte[] payload=decode85(p);
		byte[] salt=decode85(s);
		PublicKey serverKey=deserializeRawPublicKey(serverKeyBytes);
		if(privateKey==null){
			try{
				KeyFactory kf=KeyFactory.getInstance("EC");
				privateKey=kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(AccountSessionManager.getInstance().getAccount(accountID).pushPrivateKey, Base64.URL_SAFE)));
				publicKey=kf.generatePublic(new X509EncodedKeySpec(Base64.decode(AccountSessionManager.getInstance().getAccount(accountID).pushPublicKey, Base64.URL_SAFE)));
				authKey=Base64.decode(AccountSessionManager.getInstance().getAccount(accountID).pushAuthKey, Base64.URL_SAFE);
			}catch(NoSuchAlgorithmException|InvalidKeySpecException x){
				Log.e(TAG, "decryptNotification: error loading private key", x);
				return null;
			}
		}
		byte[] sharedSecret;
		try{
			KeyAgreement keyAgreement=KeyAgreement.getInstance("ECDH");
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(serverKey, true);
			sharedSecret=keyAgreement.generateSecret();
		}catch(NoSuchAlgorithmException|InvalidKeyException x){
			Log.e(TAG, "decryptNotification: error doing key exchange", x);
			return null;
		}
		byte[] secondSaltInfo="Content-Encoding: auth\0".getBytes(StandardCharsets.UTF_8);
		byte[] key, nonce;
		try{
			byte[] secondSalt=deriveKey(authKey, sharedSecret, secondSaltInfo, 32);
			byte[] keyInfo=info("aesgcm", publicKey, serverKey);
			key=deriveKey(salt, secondSalt, keyInfo, 16);
			byte[] nonceInfo=info("nonce", publicKey, serverKey);
			nonce=deriveKey(salt, secondSalt, nonceInfo, 12);
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
			decryptedStr=new String(decrypted, 2, decrypted.length-2, StandardCharsets.UTF_8);
			if(BuildConfig.DEBUG)
				Log.i(TAG, "decryptNotification: notification json "+decryptedStr);
		}catch(NoSuchAlgorithmException|NoSuchPaddingException|InvalidAlgorithmParameterException|InvalidKeyException|BadPaddingException|IllegalBlockSizeException x){
			Log.e(TAG, "decryptNotification: error decrypting payload", x);
			return null;
		}
		PushNotification notification=MastodonAPIController.gson.fromJson(decryptedStr, PushNotification.class);
		try{
			notification.postprocess();
		}catch(IOException x){
			Log.e(TAG, "decryptNotification: error verifying notification object", x);
			return null;
		}
		return notification;
	}

	private byte[] deriveKey(byte[] firstSalt, byte[] secondSalt, byte[] info, int length) throws NoSuchAlgorithmException, InvalidKeyException{
		Mac hmacContext=Mac.getInstance("HmacSHA256");
		hmacContext.init(new SecretKeySpec(firstSalt, "HmacSHA256"));
		byte[] hmac=hmacContext.doFinal(secondSalt);
		hmacContext.init(new SecretKeySpec(hmac, "HmacSHA256"));
		hmacContext.update(info);
		byte[] result=hmacContext.doFinal(new byte[]{1});
		return result.length<=length ? result : Arrays.copyOfRange(result, 0, length);
	}

	private byte[] info(String type, PublicKey clientPublicKey, PublicKey serverPublicKey){
		ByteArrayOutputStream info=new ByteArrayOutputStream();
		try{
			info.write("Content-Encoding: ".getBytes(StandardCharsets.UTF_8));
			info.write(type.getBytes(StandardCharsets.UTF_8));
			info.write(0);
			info.write("P-256".getBytes(StandardCharsets.UTF_8));
			info.write(0);
			info.write(0);
			info.write(65);
			info.write(serializeRawPublicKey(clientPublicKey));
			info.write(0);
			info.write(65);
			info.write(serializeRawPublicKey(serverPublicKey));
		}catch(IOException ignore){}
		return info.toByteArray();
	}

	private static void registerAllAccountsForPush(boolean forceReRegister){
		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			if(session.pushSubscription==null || forceReRegister)
				session.getPushSubscriptionManager().registerAccountForPush(session.pushSubscription);
			else if(session.needUpdatePushSettings)
				session.getPushSubscriptionManager().updatePushSettings(session.pushSubscription);
		}
	}

	public static class RegistrationReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent){
			if("com.google.android.c2dm.intent.REGISTRATION".equals(intent.getAction())){
				if(intent.hasExtra("registration_id")){
					deviceToken=intent.getStringExtra("registration_id");
					if(deviceToken.startsWith(KID_VALUE))
						deviceToken=deviceToken.substring(KID_VALUE.length()+1);
					getPrefs().edit().putString("deviceToken", deviceToken).putInt("version", BuildConfig.VERSION_CODE).apply();
					Log.i(TAG, "Successfully registered for FCM");
					registerAllAccountsForPush(true);
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
