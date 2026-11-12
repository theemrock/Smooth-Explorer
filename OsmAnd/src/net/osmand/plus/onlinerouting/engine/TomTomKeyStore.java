package net.osmand.plus.onlinerouting.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.smooth.routes.SecretBox;

import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Keeps the TomTom API key encrypted on the device. The AES key lives in the Android Keystore; only the
 * sealed text goes to the "smooth_tomtom_key" preferences file, which is excluded from backup and from
 * OsmAnd settings export (those cover OsmAnd's own settings files only). Reads never throw: an empty,
 * damaged or restored-on-another-device value is simply "no key".
 */
public class TomTomKeyStore {

	private static final String PREFS = "smooth_tomtom_key";
	private static final String ENTRY = "k";
	private static final String ALIAS = "smooth_tomtom_key_v1";

	private final SharedPreferences prefs;

	public TomTomKeyStore(@NonNull Context context) {
		prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	/** Call at request time and use the result only in the request URL; do not keep or log it. */
	@Nullable
	public String get() {
		try {
			String sealed = prefs.getString(ENTRY, null);
			SecretKey key = sealed == null ? null : loadKey(false);
			String value = key == null ? null : SecretBox.open(key, sealed);
			return value == null || value.isEmpty() ? null : value;
		} catch (Exception e) {
			return null;
		}
	}

	public boolean isSet() {
		return get() != null;
	}

	/** Blank input clears the key. @return false if the key could not be stored */
	public boolean set(@Nullable String value) {
		if (value == null || value.trim().isEmpty()) {
			clear();
			return true;
		}
		try {
			prefs.edit().putString(ENTRY, SecretBox.seal(loadKey(true), value.trim())).apply();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void clear() {
		prefs.edit().remove(ENTRY).apply();
	}

	@Nullable
	private static SecretKey loadKey(boolean create) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
		keyStore.load(null);
		if (keyStore.containsAlias(ALIAS)) {
			return (SecretKey) keyStore.getKey(ALIAS, null);
		}
		if (!create) {
			return null;
		}
		KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
		generator.init(new KeyGenParameterSpec.Builder(ALIAS,
				KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
				.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
				.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
				.setKeySize(256)
				.build());
		return generator.generateKey();
	}
}
