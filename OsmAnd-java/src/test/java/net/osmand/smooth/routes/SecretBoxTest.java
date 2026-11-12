package net.osmand.smooth.routes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.security.GeneralSecurityException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SecretBoxTest {

	private static SecretKey newKey() throws GeneralSecurityException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256);
		return kg.generateKey();
	}

	@Test
	public void roundTripGivesTheSecretBack() throws GeneralSecurityException {
		SecretKey key = newKey();
		String sealed = SecretBox.seal(key, "abc-DEF_123");
		assertEquals("abc-DEF_123", SecretBox.open(key, sealed));
	}

	@Test
	public void sealedFormNeverContainsThePlainSecret() throws GeneralSecurityException {
		String sealed = SecretBox.seal(newKey(), "SuperSecretKey123");
		assertFalse(sealed.contains("SuperSecretKey123"));
		assertTrue(sealed.contains(":"));
	}

	@Test
	public void sameSecretSealsDifferentlyEveryTime() throws GeneralSecurityException {
		SecretKey key = newKey();
		assertNotEquals(SecretBox.seal(key, "x"), SecretBox.seal(key, "x"));
	}

	@Test
	public void wrongKeyGivesNullNotACrash() throws GeneralSecurityException {
		String sealed = SecretBox.seal(newKey(), "x");
		assertNull(SecretBox.open(newKey(), sealed));
	}

	@Test
	public void damagedOrEmptyInputGivesNull() throws GeneralSecurityException {
		SecretKey key = newKey();
		String sealed = SecretBox.seal(key, "x");
		String tampered = sealed.substring(0, sealed.length() - 1) + (sealed.endsWith("0") ? "1" : "0");
		assertNull(SecretBox.open(key, tampered));
		assertNull(SecretBox.open(key, null));
		assertNull(SecretBox.open(key, ""));
		assertNull(SecretBox.open(key, "no-separator"));
		assertNull(SecretBox.open(key, "zz:zz"));
		assertNull(SecretBox.open(key, "abc:def"));
	}
}
