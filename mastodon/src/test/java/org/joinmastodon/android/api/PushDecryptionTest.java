package org.joinmastodon.android.api;

import org.junit.Test;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Base64;

import static org.junit.Assert.*;

public class PushDecryptionTest{
	// https://www.rfc-editor.org/rfc/rfc8291.html#appendix-A
	@Test
	public void testPushDecryption() throws Exception{
		byte[] serverKey=Base64.getUrlDecoder().decode("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
		byte[] payload=Base64.getUrlDecoder().decode("8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEsbI_0LpXMuGvnzQ");
		byte[] salt=Base64.getUrlDecoder().decode("DGv6ra1nlYgDCS1FRnbzlw");
		byte[] authSecret=Base64.getUrlDecoder().decode("BTBZMqHH6r4Tts7J_aSIgg");
		PublicKey publicKey=PushSubscriptionManager.deserializeRawPublicKey(Base64.getUrlDecoder().decode("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"));
		BigInteger priv=new BigInteger(1, Base64.getUrlDecoder().decode("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94"));
		AlgorithmParameters params=AlgorithmParameters.getInstance("EC");
		params.init(new ECGenParameterSpec("secp256r1"));
		ECParameterSpec ecParameters=params.getParameterSpec(ECParameterSpec.class);
		ECPrivateKeySpec privateKeySpec=new ECPrivateKeySpec(priv, ecParameters);
		PrivateKey privateKey=KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
		String result=PushSubscriptionManager.decryptNotification(serverKey, payload, salt, authSecret, publicKey, privateKey, true);
		assertEquals("When I grow up, I want to be a watermelon", result);
	}
}
