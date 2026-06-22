package org.joinmastodon.android.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoUtils{
	private static final SecureRandom rand=new SecureRandom();

	public static byte[] randomBytes(int length){
		byte[] b=new byte[length];
		rand.nextBytes(b);
		return b;
	}

	public static byte[] sha256(byte[] input){
		try{
			return MessageDigest.getInstance("SHA-256").digest(input);
		}catch(NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
	}
}
